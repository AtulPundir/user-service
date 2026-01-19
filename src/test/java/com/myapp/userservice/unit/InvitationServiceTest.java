package com.myapp.userservice.unit;

import com.myapp.userservice.domain.*;
import com.myapp.userservice.dto.request.OnboardUserRequest;
import com.myapp.userservice.dto.response.InvitationResponse;
import com.myapp.userservice.dto.response.OnboardUserResponse;
import com.myapp.userservice.exception.BadRequestException;
import com.myapp.userservice.exception.ConflictException;
import com.myapp.userservice.exception.NotFoundException;
import com.myapp.userservice.repository.GroupInvitationRepository;
import com.myapp.userservice.repository.UserGroupMembershipRepository;
import com.myapp.userservice.repository.UserGroupRepository;
import com.myapp.userservice.repository.UserRepository;
import com.myapp.userservice.service.InvitationService;
import com.myapp.userservice.util.CuidGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InvitationServiceTest {

    @Mock
    private GroupInvitationRepository invitationRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserGroupRepository groupRepository;

    @Mock
    private UserGroupMembershipRepository membershipRepository;

    @Mock
    private CuidGenerator cuidGenerator;

    @InjectMocks
    private InvitationService invitationService;

    private UserGroup testGroup;
    private User testUser;
    private Invitation testInvitation;

    @BeforeEach
    void setUp() {
        testGroup = new UserGroup();
        testGroup.setId("test-group-id");
        testGroup.setName("Test Group");
        testGroup.setActive(true);
        testGroup.setCreatedAt(Instant.now());
        testGroup.setUpdatedAt(Instant.now());

        testUser = new User();
        testUser.setId("test-user-id");
        testUser.setAuthUserId("test-auth-user-id");
        testUser.setName("Test User");
        testUser.setEmail("test@example.com");
        testUser.setPhone("+1234567890");
        testUser.setStatus(UserStatus.ACTIVE);
        testUser.setVerified(true);
        testUser.setCreatedAt(Instant.now());
        testUser.setUpdatedAt(Instant.now());

        testInvitation = new Invitation();
        testInvitation.setId("test-invitation-id");
        testInvitation.setGroup(testGroup);
        testInvitation.setIdentifier("+1234567890");
        testInvitation.setIdentifierType(Invitation.IdentifierType.PHONE);
        testInvitation.setInviteeName("Invited User");
        testInvitation.setStatus(InvitationStatus.PENDING);
        testInvitation.setInvitedBy("inviter-user-id");
        testInvitation.setCreatedAt(Instant.now());
        testInvitation.setExpiresAt(Instant.now().plus(30, ChronoUnit.DAYS));
    }

    @Nested
    @DisplayName("Create Invitation Tests")
    class CreateInvitationTests {

        @Test
        @DisplayName("Should create invitation successfully")
        void shouldCreateInvitationSuccessfully() {
            when(groupRepository.findById("test-group-id")).thenReturn(Optional.of(testGroup));
            when(invitationRepository.existsPendingInvitation(any(), eq("test-group-id"), any())).thenReturn(false);
            when(cuidGenerator.generate()).thenReturn("new-invitation-id");
            when(invitationRepository.save(any())).thenAnswer(inv -> {
                Invitation i = inv.getArgument(0);
                i.setCreatedAt(Instant.now());
                i.setExpiresAt(Instant.now().plus(30, ChronoUnit.DAYS));
                return i;
            });

            InvitationResponse result = invitationService.createInvitation(
                    "test-group-id", "+1234567890", "John Doe", "inviter-id");

            assertNotNull(result);
            assertEquals("new-invitation-id", result.getId());
            assertEquals("PENDING", result.getStatus());
            assertEquals("Test Group", result.getGroupName());
        }

        @Test
        @DisplayName("Should throw exception when group not found")
        void shouldThrowExceptionWhenGroupNotFound() {
            when(groupRepository.findById("non-existent")).thenReturn(Optional.empty());

            assertThrows(NotFoundException.class, () ->
                    invitationService.createInvitation("non-existent", "+1234567890", "John", "inviter"));
        }

        @Test
        @DisplayName("Should throw exception when group is inactive")
        void shouldThrowExceptionWhenGroupInactive() {
            testGroup.setActive(false);
            when(groupRepository.findById("test-group-id")).thenReturn(Optional.of(testGroup));

            assertThrows(BadRequestException.class, () ->
                    invitationService.createInvitation("test-group-id", "+1234567890", "John", "inviter"));
        }

        @Test
        @DisplayName("Should throw exception when pending invitation already exists")
        void shouldThrowExceptionWhenInvitationExists() {
            when(groupRepository.findById("test-group-id")).thenReturn(Optional.of(testGroup));
            when(invitationRepository.existsPendingInvitation(any(), eq("test-group-id"), any())).thenReturn(true);

            assertThrows(ConflictException.class, () ->
                    invitationService.createInvitation("test-group-id", "+1234567890", "John", "inviter"));
        }

        @Test
        @DisplayName("Should normalize email identifier")
        void shouldNormalizeEmailIdentifier() {
            when(groupRepository.findById("test-group-id")).thenReturn(Optional.of(testGroup));
            when(invitationRepository.existsPendingInvitation(any(), any(), any())).thenReturn(false);
            when(cuidGenerator.generate()).thenReturn("invitation-id");
            when(invitationRepository.save(any())).thenAnswer(inv -> {
                Invitation i = inv.getArgument(0);
                i.setCreatedAt(Instant.now());
                i.setExpiresAt(Instant.now().plus(30, ChronoUnit.DAYS));
                return i;
            });

            invitationService.createInvitation("test-group-id", "TEST@EXAMPLE.COM", "John", "inviter");

            ArgumentCaptor<Invitation> captor = ArgumentCaptor.forClass(Invitation.class);
            verify(invitationRepository).save(captor.capture());

            assertEquals("test@example.com", captor.getValue().getIdentifier());
            assertEquals(Invitation.IdentifierType.EMAIL, captor.getValue().getIdentifierType());
        }
    }

    @Nested
    @DisplayName("Onboard User Tests")
    class OnboardUserTests {

        @Test
        @DisplayName("Should create new user and resolve invitations")
        void shouldCreateUserAndResolveInvitations() {
            OnboardUserRequest request = new OnboardUserRequest();
            request.setAuthUserId("new-auth-user-id");
            request.setName("New User");
            request.setEmail("newuser@example.com");
            request.setPhone("+9876543210");
            request.setIsVerified(true);

            when(userRepository.findByAuthUserId("new-auth-user-id")).thenReturn(Optional.empty());
            when(userRepository.existsByEmail("newuser@example.com")).thenReturn(false);
            when(userRepository.existsByPhone("+9876543210")).thenReturn(false);
            when(userRepository.save(any())).thenAnswer(inv -> {
                User u = inv.getArgument(0);
                u.setCreatedAt(Instant.now());
                u.setUpdatedAt(Instant.now());
                return u;
            });
            when(invitationRepository.findPendingByEmailOrPhoneWithGroup(any(), any(), any()))
                    .thenReturn(Collections.singletonList(testInvitation));
            when(membershipRepository.isUserInGroup(any(), any())).thenReturn(false);
            when(cuidGenerator.generate()).thenReturn("membership-id");
            when(membershipRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(invitationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            OnboardUserResponse result = invitationService.onboardUser(request);

            assertNotNull(result);
            assertTrue(result.isUserCreated());
            assertEquals(1, result.getInvitationsResolved());
            assertNotNull(result.getUser());
            assertEquals("new-auth-user-id", result.getUser().getAuthUserId());
        }

        @Test
        @DisplayName("Should be idempotent - return existing user on retry")
        void shouldBeIdempotentOnRetry() {
            OnboardUserRequest request = new OnboardUserRequest();
            request.setAuthUserId("test-auth-user-id");
            request.setName("Test User");
            request.setEmail("test@example.com");
            request.setPhone("+1234567890");

            when(userRepository.findByAuthUserId("test-auth-user-id")).thenReturn(Optional.of(testUser));
            when(invitationRepository.findPendingByEmailOrPhoneWithGroup(any(), any(), any()))
                    .thenReturn(Collections.emptyList());

            OnboardUserResponse result = invitationService.onboardUser(request);

            assertNotNull(result);
            assertFalse(result.isUserCreated());
            assertEquals(0, result.getInvitationsResolved());
            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw exception when email already exists for different user")
        void shouldThrowExceptionWhenEmailExists() {
            OnboardUserRequest request = new OnboardUserRequest();
            request.setAuthUserId("new-auth-user-id");
            request.setName("New User");
            request.setEmail("existing@example.com");
            request.setPhone("+9999999999");

            when(userRepository.findByAuthUserId("new-auth-user-id")).thenReturn(Optional.empty());
            when(userRepository.existsByEmail("existing@example.com")).thenReturn(true);

            assertThrows(ConflictException.class, () -> invitationService.onboardUser(request));
        }

        @Test
        @DisplayName("Should resolve multiple invitations for same user")
        void shouldResolveMultipleInvitations() {
            OnboardUserRequest request = new OnboardUserRequest();
            request.setAuthUserId("new-auth-user-id");
            request.setName("New User");
            request.setEmail("newuser@example.com");
            request.setPhone("+9876543210");

            UserGroup group2 = new UserGroup();
            group2.setId("group-2-id");
            group2.setName("Group 2");
            group2.setActive(true);

            Invitation invitation2 = new Invitation();
            invitation2.setId("invitation-2-id");
            invitation2.setGroup(group2);
            invitation2.setIdentifier("newuser@example.com");
            invitation2.setIdentifierType(Invitation.IdentifierType.EMAIL);
            invitation2.setStatus(InvitationStatus.PENDING);
            invitation2.setInvitedBy("inviter-2");
            invitation2.setCreatedAt(Instant.now());
            invitation2.setExpiresAt(Instant.now().plus(30, ChronoUnit.DAYS));

            when(userRepository.findByAuthUserId("new-auth-user-id")).thenReturn(Optional.empty());
            when(userRepository.existsByEmail("newuser@example.com")).thenReturn(false);
            when(userRepository.existsByPhone("+9876543210")).thenReturn(false);
            when(userRepository.save(any())).thenAnswer(inv -> {
                User u = inv.getArgument(0);
                u.setCreatedAt(Instant.now());
                u.setUpdatedAt(Instant.now());
                return u;
            });
            when(invitationRepository.findPendingByEmailOrPhoneWithGroup(any(), any(), any()))
                    .thenReturn(Arrays.asList(testInvitation, invitation2));
            when(membershipRepository.isUserInGroup(any(), any())).thenReturn(false);
            when(cuidGenerator.generate()).thenReturn("membership-1", "membership-2");
            when(membershipRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(invitationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            OnboardUserResponse result = invitationService.onboardUser(request);

            assertNotNull(result);
            assertTrue(result.isUserCreated());
            assertEquals(2, result.getInvitationsResolved());
            verify(membershipRepository, times(2)).save(any());
        }

        @Test
        @DisplayName("Should skip invitation for inactive group")
        void shouldSkipInvitationForInactiveGroup() {
            OnboardUserRequest request = new OnboardUserRequest();
            request.setAuthUserId("new-auth-user-id");
            request.setName("New User");
            request.setEmail("newuser@example.com");
            request.setPhone("+9876543210");

            testGroup.setActive(false);

            when(userRepository.findByAuthUserId("new-auth-user-id")).thenReturn(Optional.empty());
            when(userRepository.existsByEmail("newuser@example.com")).thenReturn(false);
            when(userRepository.existsByPhone("+9876543210")).thenReturn(false);
            when(userRepository.save(any())).thenAnswer(inv -> {
                User u = inv.getArgument(0);
                u.setCreatedAt(Instant.now());
                u.setUpdatedAt(Instant.now());
                return u;
            });
            when(invitationRepository.findPendingByEmailOrPhoneWithGroup(any(), any(), any()))
                    .thenReturn(Collections.singletonList(testInvitation));
            when(invitationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            OnboardUserResponse result = invitationService.onboardUser(request);

            assertNotNull(result);
            assertTrue(result.isUserCreated());
            // Invitation is processed but marked as failed due to inactive group
            assertEquals(1, result.getResolvedInvitations().size());
            assertFalse(result.getResolvedInvitations().get(0).isMembershipCreated());
            verify(membershipRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should handle user already in group - mark invitation accepted without creating membership")
        void shouldHandleUserAlreadyInGroup() {
            OnboardUserRequest request = new OnboardUserRequest();
            request.setAuthUserId("new-auth-user-id");
            request.setName("New User");
            request.setEmail("newuser@example.com");
            request.setPhone("+9876543210");

            when(userRepository.findByAuthUserId("new-auth-user-id")).thenReturn(Optional.empty());
            when(userRepository.existsByEmail("newuser@example.com")).thenReturn(false);
            when(userRepository.existsByPhone("+9876543210")).thenReturn(false);
            when(userRepository.save(any())).thenAnswer(inv -> {
                User u = inv.getArgument(0);
                u.setCreatedAt(Instant.now());
                u.setUpdatedAt(Instant.now());
                return u;
            });
            when(invitationRepository.findPendingByEmailOrPhoneWithGroup(any(), any(), any()))
                    .thenReturn(Collections.singletonList(testInvitation));
            when(membershipRepository.isUserInGroup(any(), any())).thenReturn(true); // Already in group
            when(invitationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            OnboardUserResponse result = invitationService.onboardUser(request);

            assertNotNull(result);
            assertEquals(1, result.getInvitationsResolved());
            assertTrue(result.getResolvedInvitations().get(0).isMembershipCreated()); // Reports success
            verify(membershipRepository, never()).save(any()); // But didn't create duplicate
        }
    }

    @Nested
    @DisplayName("Cancel Invitation Tests")
    class CancelInvitationTests {

        @Test
        @DisplayName("Should cancel pending invitation")
        void shouldCancelPendingInvitation() {
            when(invitationRepository.findById("test-invitation-id")).thenReturn(Optional.of(testInvitation));
            when(invitationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            InvitationResponse result = invitationService.cancelInvitation("test-invitation-id");

            assertNotNull(result);
            assertEquals("EXPIRED", result.getStatus());
        }

        @Test
        @DisplayName("Should throw exception when invitation not found")
        void shouldThrowExceptionWhenNotFound() {
            when(invitationRepository.findById("non-existent")).thenReturn(Optional.empty());

            assertThrows(NotFoundException.class, () ->
                    invitationService.cancelInvitation("non-existent"));
        }

        @Test
        @DisplayName("Should throw exception when invitation is not pending")
        void shouldThrowExceptionWhenNotPending() {
            testInvitation.setStatus(InvitationStatus.ACCEPTED);
            when(invitationRepository.findById("test-invitation-id")).thenReturn(Optional.of(testInvitation));

            assertThrows(BadRequestException.class, () ->
                    invitationService.cancelInvitation("test-invitation-id"));
        }
    }

    @Nested
    @DisplayName("Expire Invitations Tests")
    class ExpireInvitationsTests {

        @Test
        @DisplayName("Should expire overdue invitations")
        void shouldExpireOverdueInvitations() {
            when(invitationRepository.expireOverdueInvitations(any())).thenReturn(5);

            int result = invitationService.expireOverdueInvitations();

            assertEquals(5, result);
            verify(invitationRepository).expireOverdueInvitations(any());
        }
    }

    @Nested
    @DisplayName("Get Group Invitations Tests")
    class GetGroupInvitationsTests {

        @Test
        @DisplayName("Should return all invitations for group")
        void shouldReturnAllInvitationsForGroup() {
            when(groupRepository.existsById("test-group-id")).thenReturn(true);
            when(invitationRepository.findByGroupIdOrderByCreatedAtDesc("test-group-id"))
                    .thenReturn(Collections.singletonList(testInvitation));

            List<InvitationResponse> result = invitationService.getGroupInvitations("test-group-id");

            assertNotNull(result);
            assertEquals(1, result.size());
        }

        @Test
        @DisplayName("Should throw exception when group not found")
        void shouldThrowExceptionWhenGroupNotFound() {
            when(groupRepository.existsById("non-existent")).thenReturn(false);

            assertThrows(NotFoundException.class, () ->
                    invitationService.getGroupInvitations("non-existent"));
        }

        @Test
        @DisplayName("Should return only pending invitations")
        void shouldReturnOnlyPendingInvitations() {
            when(groupRepository.existsById("test-group-id")).thenReturn(true);
            when(invitationRepository.findByGroupIdAndStatusOrderByCreatedAtDesc("test-group-id", InvitationStatus.PENDING))
                    .thenReturn(Collections.singletonList(testInvitation));

            List<InvitationResponse> result = invitationService.getPendingGroupInvitations("test-group-id");

            assertNotNull(result);
            assertEquals(1, result.size());
        }
    }
}
