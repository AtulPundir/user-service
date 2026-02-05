package com.myapp.identityservice.service;

import com.myapp.identityservice.event.EventPublisher;
import com.myapp.identityservice.domain.User;
import com.myapp.identityservice.domain.UserStatus;
import com.myapp.identityservice.dto.request.CreateUserRequest;
import com.myapp.identityservice.dto.request.UpdateUserRequest;
import com.myapp.identityservice.dto.request.UserFilterRequest;
import com.myapp.identityservice.dto.response.ApiResponse;
import com.myapp.identityservice.dto.response.PaginationInfo;
import com.myapp.identityservice.dto.response.UsageResponse;
import com.myapp.identityservice.dto.response.UserResponse;
import com.myapp.identityservice.exception.BadRequestException;
import com.myapp.identityservice.exception.ConflictException;
import com.myapp.identityservice.exception.NotFoundException;
import com.myapp.identityservice.repository.UserMonthlyUsageRepository;
import com.myapp.identityservice.repository.UserRepository;
import com.myapp.identityservice.util.CuidGenerator;
import lombok.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class UserService {

    private static final Logger logger = LoggerFactory.getLogger(UserService.class);
    private static final int DEFAULT_MONTHLY_TASK_LIMIT = 50;

    private final UserRepository userRepository;
    private final UserMonthlyUsageRepository usageRepository;
    private final CuidGenerator cuidGenerator;
    private final EventPublisher eventPublisher;

    public UserService(UserRepository userRepository,
                      UserMonthlyUsageRepository usageRepository,
                      CuidGenerator cuidGenerator,
                      EventPublisher eventPublisher) {
        this.userRepository = userRepository;
        this.usageRepository = usageRepository;
        this.cuidGenerator = cuidGenerator;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public UserResponse createUser(CreateUserRequest request) {
        String effectiveId = validateRequest(request);

        User user = new User();
        user.setId(effectiveId);
        user.setAuthUserId(effectiveId);
        user.setName(request.getName());
        user.setEmail(request.getEmail());
        user.setPhone(request.getPhone());
        user.setVerified(request.getIsVerified() != null ? request.getIsVerified() : false);
        user.setStatus(UserStatus.ACTIVE);
        user.setDefaultMonthlyTaskLimit(
                request.getDefaultMonthlyTaskLimit() != null
                        ? request.getDefaultMonthlyTaskLimit()
                        : DEFAULT_MONTHLY_TASK_LIMIT
        );
        user.setSubscriptionPlanId(request.getSubscriptionPlanId());

        User savedUser = userRepository.save(user);
        logger.info("User created: id={}, email={}", savedUser.getId(), maskEmail(savedUser.getEmail()));

        return UserResponse.fromEntity(savedUser);
    }

    private @NonNull String validateRequest(CreateUserRequest request) {
        // Validate that either id or authUserId is provided
        if (!request.hasValidId()) {
            throw new BadRequestException("Either id or authUserId must be provided");
        }

        String effectiveId = request.getEffectiveId();

        // Check for duplicate id
        if (userRepository.existsById(effectiveId)) {
            throw ConflictException.userIdExists();
        }

        // Check for duplicate email
        if (userRepository.existsByEmail(request.getEmail())) {
            throw ConflictException.emailExists();
        }

        // Check for duplicate phone (normalize before checking)
        String normalizedPhone = normalizePhone(request.getPhone());
        if (normalizedPhone != null && userRepository.existsByPhone(normalizedPhone)) {
            throw ConflictException.phoneExists();
        }
        return effectiveId;
    }

    @Transactional(readOnly = true)
    public UserResponse getUserById(String id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> NotFoundException.user(id));

        List<UsageResponse> usage = usageRepository.findLast12MonthsByUserId(id)
                .stream()
                .map(UsageResponse::fromEntity)
                .collect(Collectors.toList());

        return UserResponse.fromEntityWithUsage(user, usage);
    }

    @Transactional(readOnly = true)
    public UserResponse getCurrentUser(String userId) {
        return getUserById(userId);
    }

    @Transactional(readOnly = true)
    public ApiResponse<List<UserResponse>> listUsers(int page, int limit, UserFilterRequest filter) {
        Pageable pageable = PageRequest.of(page - 1, limit);

        Page<User> userPage = findUsersWithFilters(filter, pageable);

        List<UserResponse> users = userPage.getContent()
                .stream()
                .map(UserResponse::fromEntity)
                .collect(Collectors.toList());

        PaginationInfo pagination = new PaginationInfo(page, limit, userPage.getTotalElements());

        return ApiResponse.success(users, pagination);
    }

    private Page<User> findUsersWithFilters(UserFilterRequest filter, Pageable pageable) {
        UserStatus status = filter != null ? filter.getStatus() : null;
        Boolean verified = filter != null ? filter.getVerified() : null;

        if (status != null && verified != null) {
            return userRepository.findByStatusAndIsVerifiedOrderByCreatedAtDesc(status, verified, pageable);
        } else if (status != null) {
            return userRepository.findByStatusOrderByCreatedAtDesc(status, pageable);
        } else if (verified != null) {
            return userRepository.findByIsVerifiedOrderByCreatedAtDesc(verified, pageable);
        } else {
            return userRepository.findAllOrderByCreatedAtDesc(pageable);
        }
    }

    @Transactional
    public UserResponse updateUser(String id, UpdateUserRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> NotFoundException.user(id));

        boolean nameChanged = false;
        if (request.getName() != null && !request.getName().equals(user.getName())) {
            user.setName(request.getName());
            nameChanged = true;
        }

        if (request.getEmail() != null && !request.getEmail().equals(user.getEmail())) {
            if (userRepository.existsByEmailAndIdNot(request.getEmail(), id)) {
                throw ConflictException.emailInUse();
            }
            user.setEmail(request.getEmail());
        }

        if (request.getStatus() != null) {
            user.setStatus(request.getStatus());
        }

        User savedUser = userRepository.save(user);
        logger.info("User updated: id={}", savedUser.getId());

        if (nameChanged) {
            eventPublisher.publishUserNameUpdated(
                    savedUser.getId(), savedUser.getName(),
                    savedUser.getUpdatedAt().toEpochMilli());
        }

        return UserResponse.fromEntity(savedUser);
    }

    @Transactional
    public UserResponse deleteUser(String id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> NotFoundException.user(id));

        user.setStatus(UserStatus.DELETED);
        User savedUser = userRepository.save(user);
        logger.info("User deleted (soft): id={}", savedUser.getId());

        return UserResponse.fromEntity(savedUser);
    }

    @Transactional(readOnly = true)
    public User findByPhone(String phone) {
        return userRepository.findByPhone(phone).orElse(null);
    }

    @Transactional(readOnly = true)
    public User findByAuthUserId(String authUserId) {
        return userRepository.findByAuthUserId(authUserId).orElse(null);
    }

    @Transactional(readOnly = true)
    public User findById(String id) {
        return userRepository.findById(id).orElse(null);
    }

    /**
     * @deprecated This method creates shadow users which violates the principle that
     * users should only be created through the Auth Service. Use the invitation system
     * instead - when a user doesn't exist, create a GroupInvitation via InvitationService.
     * This method is kept temporarily for backward compatibility but should not be used.
     * @see com.myapp.identityservice.service.InvitationService
     */
    @Deprecated(forRemoval = true)
    @Transactional
    public User createUnverifiedUser(String name, String phone) {
        throw new UnsupportedOperationException(
            "Creating unverified users is no longer supported. " +
            "Use the invitation system instead. Non-registered users should be invited, " +
            "and they will be added to groups when they sign up via Auth Service."
        );
    }

    @Transactional(readOnly = true)
    public User findByEmail(String email) {
        return userRepository.findByEmail(email).orElse(null);
    }

    @Transactional
    public void updateSubscription(String authUserId, String planId, int monthlyLimit) {
        User user = userRepository.findByAuthUserId(authUserId)
                .orElseThrow(() -> NotFoundException.userWithAuthId(authUserId));

        user.setSubscriptionPlanId(planId);
        user.setDefaultMonthlyTaskLimit(monthlyLimit);
        userRepository.save(user);

        logger.info("Subscription updated: userId={}, planId={}, limit={}", user.getId(), planId, monthlyLimit);
    }

    private String maskEmail(String email) {
        if (email == null || email.length() < 3) {
            return "***";
        }
        int atIndex = email.indexOf('@');
        if (atIndex <= 2) {
            return email.substring(0, 1) + "***" + email.substring(atIndex);
        }
        return email.substring(0, 2) + "***" + email.substring(atIndex);
    }

    /**
     * Normalize phone number to +{countrycode}{number} format.
     */
    private String normalizePhone(String phone) {
        if (phone == null || phone.isBlank()) {
            return null;
        }
        String normalized = phone.replaceAll("[^0-9+]", "");

        // Ensure + prefix for E.164 format
        if (!normalized.startsWith("+")) {
            normalized = "+" + normalized;
        }
        return normalized;
    }
}
