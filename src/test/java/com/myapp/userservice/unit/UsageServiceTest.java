package com.myapp.userservice.unit;

import com.myapp.userservice.domain.User;
import com.myapp.userservice.domain.UserMonthlyUsage;
import com.myapp.userservice.domain.UserStatus;
import com.myapp.userservice.dto.response.UsageCheckResponse;
import com.myapp.userservice.dto.response.UsageConsumeResponse;
import com.myapp.userservice.dto.response.UsageResponse;
import com.myapp.userservice.exception.BadRequestException;
import com.myapp.userservice.exception.NotFoundException;
import com.myapp.userservice.exception.UsageLimitExceededException;
import com.myapp.userservice.repository.UserMonthlyUsageRepository;
import com.myapp.userservice.repository.UserRepository;
import com.myapp.userservice.service.UsageService;
import com.myapp.userservice.util.CuidGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UsageServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserMonthlyUsageRepository usageRepository;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private CuidGenerator cuidGenerator;

    private UsageService usageService;

    private User testUser;
    private UserMonthlyUsage testUsage;

    @BeforeEach
    void setUp() {
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        usageService = new UsageService(userRepository, usageRepository, redisTemplate, cuidGenerator);

        testUser = new User();
        testUser.setId("test-user-id");
        testUser.setAuthUserId("test-user-id");
        testUser.setName("Test User");
        testUser.setEmail("test@example.com");
        testUser.setPhone("+1234567890");
        testUser.setStatus(UserStatus.ACTIVE);
        testUser.setDefaultMonthlyTaskLimit(100);
        testUser.setCreatedAt(Instant.now());
        testUser.setUpdatedAt(Instant.now());

        LocalDate now = LocalDate.now();
        testUsage = new UserMonthlyUsage();
        testUsage.setId("test-usage-id");
        testUsage.setUser(testUser);
        testUsage.setYear(now.getYear());
        testUsage.setMonth(now.getMonthValue());
        testUsage.setMonthlyLimit(100);
        testUsage.setUtilised(50);
        testUsage.setCreatedAt(Instant.now());
        testUsage.setUpdatedAt(Instant.now());
    }

    @Nested
    @DisplayName("Check Usage Tests")
    class CheckUsageTests {

        @Test
        @DisplayName("Should return allowed when within limit")
        void shouldReturnAllowedWhenWithinLimit() {
            when(userRepository.findById("test-user-id")).thenReturn(Optional.of(testUser));
            when(usageRepository.findByUserIdAndYearAndMonth(anyString(), anyInt(), anyInt()))
                    .thenReturn(Optional.of(testUsage));

            UsageCheckResponse result = usageService.checkUsage("test-user-id", 25);

            assertTrue(result.isAllowed());
            assertEquals(100, result.getMonthlyLimit());
            assertEquals(50, result.getUtilised());
            assertEquals(50, result.getRemaining());
            assertFalse(result.isUnlimited());
        }

        @Test
        @DisplayName("Should return not allowed when exceeds limit")
        void shouldReturnNotAllowedWhenExceedsLimit() {
            when(userRepository.findById("test-user-id")).thenReturn(Optional.of(testUser));
            when(usageRepository.findByUserIdAndYearAndMonth(anyString(), anyInt(), anyInt()))
                    .thenReturn(Optional.of(testUsage));

            UsageCheckResponse result = usageService.checkUsage("test-user-id", 60);

            assertFalse(result.isAllowed());
            assertEquals(50, result.getRemaining());
        }

        @Test
        @DisplayName("Should return allowed for unlimited plan")
        void shouldReturnAllowedForUnlimitedPlan() {
            testUsage.setMonthlyLimit(-1);

            when(userRepository.findById("test-user-id")).thenReturn(Optional.of(testUser));
            when(usageRepository.findByUserIdAndYearAndMonth(anyString(), anyInt(), anyInt()))
                    .thenReturn(Optional.of(testUsage));

            UsageCheckResponse result = usageService.checkUsage("test-user-id", 1000);

            assertTrue(result.isAllowed());
            assertTrue(result.isUnlimited());
            assertEquals(-1, result.getRemaining());
        }

        @Test
        @DisplayName("Should throw exception for invalid amount")
        void shouldThrowExceptionForInvalidAmount() {
            assertThrows(BadRequestException.class, () -> usageService.checkUsage("test-user-id", 0));
            assertThrows(BadRequestException.class, () -> usageService.checkUsage("test-user-id", -1));
        }

        @Test
        @DisplayName("Should throw exception when user not found")
        void shouldThrowExceptionWhenUserNotFound() {
            when(userRepository.findById("non-existent")).thenReturn(Optional.empty());

            assertThrows(NotFoundException.class, () -> usageService.checkUsage("non-existent", 10));
        }
    }

    @Nested
    @DisplayName("Consume Usage Tests")
    class ConsumeUsageTests {

        @Test
        @DisplayName("Should consume usage successfully")
        void shouldConsumeUsageSuccessfully() {
            when(valueOperations.setIfAbsent(anyString(), anyString(), any(Duration.class))).thenReturn(true);
            when(valueOperations.get(anyString())).thenReturn(null);
            when(userRepository.findById("test-user-id")).thenReturn(Optional.of(testUser));
            when(usageRepository.findByUserIdAndYearAndMonthWithLock(anyString(), anyInt(), anyInt()))
                    .thenReturn(Optional.of(testUsage));
            when(usageRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            UsageConsumeResponse result = usageService.consumeUsage("test-user-id", 10, null);

            assertTrue(result.isSuccess());
            assertEquals(100, result.getMonthlyLimit());
            assertEquals(60, result.getUtilised());
            assertEquals(40, result.getRemaining());
        }

        @Test
        @DisplayName("Should throw exception when limit exceeded")
        void shouldThrowExceptionWhenLimitExceeded() {
            when(valueOperations.setIfAbsent(anyString(), anyString(), any(Duration.class))).thenReturn(true);
            when(valueOperations.get(anyString())).thenReturn(null);
            when(userRepository.findById("test-user-id")).thenReturn(Optional.of(testUser));
            when(usageRepository.findByUserIdAndYearAndMonthWithLock(anyString(), anyInt(), anyInt()))
                    .thenReturn(Optional.of(testUsage));

            assertThrows(UsageLimitExceededException.class,
                    () -> usageService.consumeUsage("test-user-id", 60, null));
        }

        @Test
        @DisplayName("Should throw exception when lock not acquired")
        void shouldThrowExceptionWhenLockNotAcquired() {
            when(valueOperations.setIfAbsent(anyString(), anyString(), any(Duration.class))).thenReturn(false);

            assertThrows(BadRequestException.class,
                    () -> usageService.consumeUsage("test-user-id", 10, null));
        }

        @Test
        @DisplayName("Should allow unlimited usage")
        void shouldAllowUnlimitedUsage() {
            testUsage.setMonthlyLimit(-1);

            when(valueOperations.setIfAbsent(anyString(), anyString(), any(Duration.class))).thenReturn(true);
            when(valueOperations.get(anyString())).thenReturn(null);
            when(userRepository.findById("test-user-id")).thenReturn(Optional.of(testUser));
            when(usageRepository.findByUserIdAndYearAndMonthWithLock(anyString(), anyInt(), anyInt()))
                    .thenReturn(Optional.of(testUsage));
            when(usageRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            UsageConsumeResponse result = usageService.consumeUsage("test-user-id", 1000, null);

            assertTrue(result.isSuccess());
            assertTrue(result.isUnlimited());
            assertEquals(-1, result.getRemaining());
        }
    }

    @Nested
    @DisplayName("Get Monthly Usage Tests")
    class GetMonthlyUsageTests {

        @Test
        @DisplayName("Should get current month usage")
        void shouldGetCurrentMonthUsage() {
            when(userRepository.findById("test-user-id")).thenReturn(Optional.of(testUser));
            when(usageRepository.findByUserIdAndYearAndMonth(anyString(), anyInt(), anyInt()))
                    .thenReturn(Optional.of(testUsage));

            UsageResponse result = usageService.getCurrentMonthUsage("test-user-id");

            assertNotNull(result);
            assertEquals(100, result.getMonthlyLimit());
            assertEquals(50, result.getUtilised());
        }

        @Test
        @DisplayName("Should return default usage when no record exists")
        void shouldReturnDefaultUsageWhenNoRecordExists() {
            when(userRepository.findById("test-user-id")).thenReturn(Optional.of(testUser));
            when(usageRepository.findByUserIdAndYearAndMonth(anyString(), anyInt(), anyInt()))
                    .thenReturn(Optional.empty());

            UsageResponse result = usageService.getCurrentMonthUsage("test-user-id");

            assertNotNull(result);
            assertEquals(100, result.getMonthlyLimit());
            assertEquals(0, result.getUtilised());
        }

        @Test
        @DisplayName("Should throw exception for invalid month")
        void shouldThrowExceptionForInvalidMonth() {
            assertThrows(BadRequestException.class,
                    () -> usageService.getMonthlyUsage("test-user-id", 2024, 0));
            assertThrows(BadRequestException.class,
                    () -> usageService.getMonthlyUsage("test-user-id", 2024, 13));
        }
    }
}
