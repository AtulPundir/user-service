package com.myapp.userservice.unit;

import com.myapp.userservice.domain.User;
import com.myapp.userservice.domain.UserStatus;
import com.myapp.userservice.dto.request.CreateUserRequest;
import com.myapp.userservice.dto.request.UpdateUserRequest;
import com.myapp.userservice.dto.response.UserResponse;
import com.myapp.userservice.exception.BadRequestException;
import com.myapp.userservice.exception.ConflictException;
import com.myapp.userservice.exception.NotFoundException;
import com.myapp.userservice.repository.UserMonthlyUsageRepository;
import com.myapp.userservice.repository.UserRepository;
import com.myapp.userservice.service.UserService;
import com.myapp.userservice.util.CuidGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserMonthlyUsageRepository usageRepository;

    @Mock
    private CuidGenerator cuidGenerator;

    @InjectMocks
    private UserService userService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId("test-user-id");
        testUser.setAuthUserId("test-user-id");
        testUser.setName("Test User");
        testUser.setEmail("test@example.com");
        testUser.setPhone("+1234567890");
        testUser.setVerified(false);
        testUser.setStatus(UserStatus.ACTIVE);
        testUser.setDefaultMonthlyTaskLimit(50);
        testUser.setCreatedAt(Instant.now());
        testUser.setUpdatedAt(Instant.now());
    }

    @Nested
    @DisplayName("Create User Tests")
    class CreateUserTests {

        @Test
        @DisplayName("Should create user successfully with valid data")
        void shouldCreateUserSuccessfully() {
            CreateUserRequest request = new CreateUserRequest();
            request.setId("new-user-id");
            request.setName("New User");
            request.setEmail("new@example.com");
            request.setPhone("+9876543210");

            when(userRepository.existsById(any())).thenReturn(false);
            when(userRepository.existsByEmail(any())).thenReturn(false);
            when(userRepository.existsByPhone(any())).thenReturn(false);
            when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            UserResponse result = userService.createUser(request);

            assertNotNull(result);
            assertEquals("new-user-id", result.getId());
            assertEquals("New User", result.getName());
            assertEquals("new@example.com", result.getEmail());
            assertEquals(UserStatus.ACTIVE, result.getStatus());
            verify(userRepository).save(any(User.class));
        }

        @Test
        @DisplayName("Should throw exception when neither id nor authUserId provided")
        void shouldThrowExceptionWhenNoIdProvided() {
            CreateUserRequest request = new CreateUserRequest();
            request.setName("New User");
            request.setEmail("new@example.com");
            request.setPhone("+9876543210");

            assertThrows(BadRequestException.class, () -> userService.createUser(request));
            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw exception when user ID already exists")
        void shouldThrowExceptionWhenUserIdExists() {
            CreateUserRequest request = new CreateUserRequest();
            request.setId("existing-id");
            request.setName("New User");
            request.setEmail("new@example.com");
            request.setPhone("+9876543210");

            when(userRepository.existsById("existing-id")).thenReturn(true);

            assertThrows(ConflictException.class, () -> userService.createUser(request));
        }

        @Test
        @DisplayName("Should throw exception when email already exists")
        void shouldThrowExceptionWhenEmailExists() {
            CreateUserRequest request = new CreateUserRequest();
            request.setId("new-id");
            request.setName("New User");
            request.setEmail("existing@example.com");
            request.setPhone("+9876543210");

            when(userRepository.existsById(any())).thenReturn(false);
            when(userRepository.existsByEmail("existing@example.com")).thenReturn(true);

            assertThrows(ConflictException.class, () -> userService.createUser(request));
        }

        @Test
        @DisplayName("Should throw exception when phone already exists")
        void shouldThrowExceptionWhenPhoneExists() {
            CreateUserRequest request = new CreateUserRequest();
            request.setId("new-id");
            request.setName("New User");
            request.setEmail("new@example.com");
            request.setPhone("+1234567890");

            when(userRepository.existsById(any())).thenReturn(false);
            when(userRepository.existsByEmail(any())).thenReturn(false);
            when(userRepository.existsByPhone("+1234567890")).thenReturn(true);

            assertThrows(ConflictException.class, () -> userService.createUser(request));
        }

        @Test
        @DisplayName("Should use authUserId when id is not provided")
        void shouldUseAuthUserIdWhenIdNotProvided() {
            CreateUserRequest request = new CreateUserRequest();
            request.setAuthUserId("auth-user-id");
            request.setName("New User");
            request.setEmail("new@example.com");
            request.setPhone("+9876543210");

            when(userRepository.existsById("auth-user-id")).thenReturn(false);
            when(userRepository.existsByEmail(any())).thenReturn(false);
            when(userRepository.existsByPhone(any())).thenReturn(false);
            when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            UserResponse result = userService.createUser(request);

            assertEquals("auth-user-id", result.getId());
            assertEquals("auth-user-id", result.getAuthUserId());
        }
    }

    @Nested
    @DisplayName("Get User Tests")
    class GetUserTests {

        @Test
        @DisplayName("Should get user by ID successfully")
        void shouldGetUserByIdSuccessfully() {
            when(userRepository.findById("test-user-id")).thenReturn(Optional.of(testUser));
            when(usageRepository.findLast12MonthsByUserId(any())).thenReturn(Collections.emptyList());

            UserResponse result = userService.getUserById("test-user-id");

            assertNotNull(result);
            assertEquals("test-user-id", result.getId());
            assertEquals("Test User", result.getName());
        }

        @Test
        @DisplayName("Should throw exception when user not found")
        void shouldThrowExceptionWhenUserNotFound() {
            when(userRepository.findById("non-existent")).thenReturn(Optional.empty());

            assertThrows(NotFoundException.class, () -> userService.getUserById("non-existent"));
        }
    }

    @Nested
    @DisplayName("Update User Tests")
    class UpdateUserTests {

        @Test
        @DisplayName("Should update user name successfully")
        void shouldUpdateUserNameSuccessfully() {
            UpdateUserRequest request = new UpdateUserRequest();
            request.setName("Updated Name");

            when(userRepository.findById("test-user-id")).thenReturn(Optional.of(testUser));
            when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            UserResponse result = userService.updateUser("test-user-id", request);

            assertEquals("Updated Name", result.getName());
        }

        @Test
        @DisplayName("Should update user email successfully")
        void shouldUpdateUserEmailSuccessfully() {
            UpdateUserRequest request = new UpdateUserRequest();
            request.setEmail("updated@example.com");

            when(userRepository.findById("test-user-id")).thenReturn(Optional.of(testUser));
            when(userRepository.existsByEmailAndIdNot("updated@example.com", "test-user-id")).thenReturn(false);
            when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            UserResponse result = userService.updateUser("test-user-id", request);

            assertEquals("updated@example.com", result.getEmail());
        }

        @Test
        @DisplayName("Should throw exception when updating to existing email")
        void shouldThrowExceptionWhenUpdatingToExistingEmail() {
            UpdateUserRequest request = new UpdateUserRequest();
            request.setEmail("existing@example.com");

            when(userRepository.findById("test-user-id")).thenReturn(Optional.of(testUser));
            when(userRepository.existsByEmailAndIdNot("existing@example.com", "test-user-id")).thenReturn(true);

            assertThrows(ConflictException.class, () -> userService.updateUser("test-user-id", request));
        }

        @Test
        @DisplayName("Should update user status successfully")
        void shouldUpdateUserStatusSuccessfully() {
            UpdateUserRequest request = new UpdateUserRequest();
            request.setStatus(UserStatus.INACTIVE);

            when(userRepository.findById("test-user-id")).thenReturn(Optional.of(testUser));
            when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            UserResponse result = userService.updateUser("test-user-id", request);

            assertEquals(UserStatus.INACTIVE, result.getStatus());
        }
    }

    @Nested
    @DisplayName("Delete User Tests")
    class DeleteUserTests {

        @Test
        @DisplayName("Should soft delete user successfully")
        void shouldSoftDeleteUserSuccessfully() {
            when(userRepository.findById("test-user-id")).thenReturn(Optional.of(testUser));
            when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            UserResponse result = userService.deleteUser("test-user-id");

            assertEquals(UserStatus.DELETED, result.getStatus());
        }

        @Test
        @DisplayName("Should throw exception when deleting non-existent user")
        void shouldThrowExceptionWhenDeletingNonExistentUser() {
            when(userRepository.findById("non-existent")).thenReturn(Optional.empty());

            assertThrows(NotFoundException.class, () -> userService.deleteUser("non-existent"));
        }
    }
}
