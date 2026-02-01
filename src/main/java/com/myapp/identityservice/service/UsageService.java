package com.myapp.identityservice.service;

import com.myapp.identityservice.domain.User;
import com.myapp.identityservice.domain.UserMonthlyUsage;
import com.myapp.identityservice.dto.response.UsageCheckResponse;
import com.myapp.identityservice.dto.response.UsageConsumeResponse;
import com.myapp.identityservice.dto.response.UsageResponse;
import com.myapp.identityservice.exception.BadRequestException;
import com.myapp.identityservice.exception.NotFoundException;
import com.myapp.identityservice.exception.UsageLimitExceededException;
import com.myapp.identityservice.repository.UserMonthlyUsageRepository;
import com.myapp.identityservice.repository.UserRepository;
import com.myapp.identityservice.util.CuidGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class UsageService {

    private static final Logger logger = LoggerFactory.getLogger(UsageService.class);
    private static final String LOCK_KEY_PREFIX = "usage_lock:";
    private static final String IDEMPOTENCY_KEY_PREFIX = "idempotency:";
    private static final long LOCK_TTL_MS = 5000;
    private static final long IDEMPOTENCY_TTL_SECONDS = 86400; // 24 hours

    private final UserRepository userRepository;
    private final UserMonthlyUsageRepository usageRepository;
    private final StringRedisTemplate redisTemplate;
    private final CuidGenerator cuidGenerator;

    public UsageService(UserRepository userRepository,
                       UserMonthlyUsageRepository usageRepository,
                       StringRedisTemplate redisTemplate,
                       CuidGenerator cuidGenerator) {
        this.userRepository = userRepository;
        this.usageRepository = usageRepository;
        this.redisTemplate = redisTemplate;
        this.cuidGenerator = cuidGenerator;
    }

    @Transactional(readOnly = true)
    public UsageCheckResponse checkUsage(String userId, int amount) {
        if (amount <= 0) {
            throw new BadRequestException("Amount must be greater than 0");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> NotFoundException.user(userId));

        LocalDate now = LocalDate.now();
        int year = now.getYear();
        int month = now.getMonthValue();

        UserMonthlyUsage usage = getOrCreateUsage(user, year, month);

        return UsageCheckResponse.from(usage.getMonthlyLimit(), usage.getUtilised(), amount);
    }

    @Transactional
    public UsageConsumeResponse consumeUsage(String userId, int amount, String idempotencyKey) {
        if (amount <= 0) {
            throw new BadRequestException("Amount must be greater than 0");
        }

        // Check idempotency cache
        if (idempotencyKey != null) {
            String cachedResult = redisTemplate.opsForValue().get(IDEMPOTENCY_KEY_PREFIX + idempotencyKey);
            if (cachedResult != null) {
                logger.info("Returning cached result for idempotency key: {}", idempotencyKey);
                return parseConsumeResponse(cachedResult);
            }
        }

        LocalDate now = LocalDate.now();
        int year = now.getYear();
        int month = now.getMonthValue();

        String lockKey = LOCK_KEY_PREFIX + userId + ":" + year + ":" + month;
        String lockValue = UUID.randomUUID().toString();

        // Try to acquire distributed lock
        Boolean acquired = redisTemplate.opsForValue()
                .setIfAbsent(lockKey, lockValue, Duration.ofMillis(LOCK_TTL_MS));

        if (acquired == null || !acquired) {
            throw new BadRequestException("Another operation in progress, please retry");
        }

        try {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> NotFoundException.user(userId));

            UserMonthlyUsage usage = getOrCreateUsageWithLock(user, year, month);

            // Check if unlimited
            boolean isUnlimited = usage.getMonthlyLimit() < 0;

            if (!isUnlimited) {
                int newUtilised = usage.getUtilised() + amount;
                if (newUtilised > usage.getMonthlyLimit()) {
                    logger.warn("Usage limit exceeded: userId={}, requested={}, available={}",
                            userId, amount, usage.getRemaining());
                    throw new UsageLimitExceededException(
                            usage.getMonthlyLimit(),
                            usage.getUtilised(),
                            amount
                    );
                }
            }

            // Update usage
            usage.setUtilised(usage.getUtilised() + amount);
            usageRepository.save(usage);

            UsageConsumeResponse response = UsageConsumeResponse.from(
                    usage.getMonthlyLimit(),
                    usage.getUtilised()
            );

            // Cache result if idempotency key provided
            if (idempotencyKey != null) {
                String cacheValue = serializeConsumeResponse(response);
                redisTemplate.opsForValue().set(
                        IDEMPOTENCY_KEY_PREFIX + idempotencyKey,
                        cacheValue,
                        IDEMPOTENCY_TTL_SECONDS,
                        TimeUnit.SECONDS
                );
            }

            logger.info("Usage consumed: userId={}, amount={}, remaining={}",
                    userId, amount, response.getRemaining());

            return response;

        } finally {
            // Release lock if still owned
            String currentLockValue = redisTemplate.opsForValue().get(lockKey);
            if (lockValue.equals(currentLockValue)) {
                redisTemplate.delete(lockKey);
            }
        }
    }

    @Transactional(readOnly = true)
    public UsageResponse getCurrentMonthUsage(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> NotFoundException.user(userId));

        LocalDate now = LocalDate.now();
        int year = now.getYear();
        int month = now.getMonthValue();

        return usageRepository.findByUserIdAndYearAndMonth(userId, year, month)
                .map(UsageResponse::fromEntity)
                .orElseGet(() -> UsageResponse.defaultUsage(userId, year, month, user.getDefaultMonthlyTaskLimit()));
    }

    @Transactional(readOnly = true)
    public UsageResponse getMonthlyUsage(String userId, int year, int month) {
        if (month < 1 || month > 12) {
            throw new BadRequestException("Month must be between 1 and 12");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> NotFoundException.user(userId));

        return usageRepository.findByUserIdAndYearAndMonth(userId, year, month)
                .map(UsageResponse::fromEntity)
                .orElseThrow(NotFoundException::usage);
    }

    @Transactional(readOnly = true)
    public List<UsageResponse> getYearlyUsage(String userId, int year) {
        if (!userRepository.existsById(userId)) {
            throw NotFoundException.user(userId);
        }

        return usageRepository.findByUserIdAndYear(userId, year)
                .stream()
                .map(UsageResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional
    public void updateMonthlyLimit(String userId, int newLimit) {
        LocalDate now = LocalDate.now();
        int year = now.getYear();
        int month = now.getMonthValue();

        usageRepository.findByUserIdAndYearAndMonth(userId, year, month)
                .ifPresent(usage -> {
                    usage.setMonthlyLimit(newLimit);
                    usageRepository.save(usage);
                    logger.info("Monthly limit updated: userId={}, year={}, month={}, newLimit={}",
                            userId, year, month, newLimit);
                });
    }

    private UserMonthlyUsage getOrCreateUsage(User user, int year, int month) {
        return usageRepository.findByUserIdAndYearAndMonth(user.getId(), year, month)
                .orElseGet(() -> createUsageRecord(user, year, month));
    }

    private UserMonthlyUsage getOrCreateUsageWithLock(User user, int year, int month) {
        return usageRepository.findByUserIdAndYearAndMonthWithLock(user.getId(), year, month)
                .orElseGet(() -> createUsageRecord(user, year, month));
    }

    private UserMonthlyUsage createUsageRecord(User user, int year, int month) {
        UserMonthlyUsage usage = new UserMonthlyUsage();
        usage.setId(cuidGenerator.generate());
        usage.setUser(user);
        usage.setYear(year);
        usage.setMonth(month);
        usage.setMonthlyLimit(user.getDefaultMonthlyTaskLimit());
        usage.setUtilised(0);

        return usageRepository.save(usage);
    }

    private String serializeConsumeResponse(UsageConsumeResponse response) {
        return String.format("%d|%d|%d|%b",
                response.getMonthlyLimit(),
                response.getUtilised(),
                response.getRemaining(),
                response.isUnlimited()
        );
    }

    private UsageConsumeResponse parseConsumeResponse(String cached) {
        String[] parts = cached.split("\\|");
        UsageConsumeResponse response = new UsageConsumeResponse();
        response.setSuccess(true);
        response.setMonthlyLimit(Integer.parseInt(parts[0]));
        response.setUtilised(Integer.parseInt(parts[1]));
        response.setRemaining(Integer.parseInt(parts[2]));
        response.setUnlimited(Boolean.parseBoolean(parts[3]));
        return response;
    }
}
