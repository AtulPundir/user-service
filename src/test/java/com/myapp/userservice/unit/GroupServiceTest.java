package com.myapp.userservice.unit;

import com.myapp.userservice.domain.*;
import com.myapp.userservice.dto.request.CreateGroupWithUsersRequest;
import com.myapp.userservice.dto.response.CreateGroupWithUsersResponse;
import com.myapp.userservice.dto.response.MembershipResponse;
import com.myapp.userservice.exception.BadRequestException;
import com.myapp.userservice.exception.ConflictException;
import com.myapp.userservice.exception.NotFoundException;
import com.myapp.userservice.repository.UserGroupMembershipRepository;
import com.myapp.userservice.repository.UserGroupRepository;
import com.myapp.userservice.repository.UserRepository;
import com.myapp.userservice.service.GroupService;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GroupServiceTest {

    @Mock
    private UserGroupRepository groupRepository;

    @Mock
    private UserGroupMembershipRepository membershipRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserService userService;

    @Mock
    private CuidGenerator cuidGenerator;

    @InjectMocks
    private GroupService groupService;

    private UserGroup testGroup;
    private User testUser;

    @BeforeEach
    void setUp() {
        testGroup = new UserGroup();
        testGroup.setId("test-group-id");
        testGroup.setName("Test Group");
        testGroup.setDescription("Test Description");
        testGroup.setActive(true);
        testGroup.setCreatedAt(Instant.now());
        testGroup.setUpdatedAt(Instant.now());

        testUser = new User();
        testUser.setId("test-user-id");
        testUser.setAuthUserId("test-user-id");
        testUser.setName("Test User");
        testUser.setEmail("test@example.com");
        testUser.setPhone("+1234567890");
        testUser.setStatus(UserStatus.ACTIVE);
        testUser.setDefaultMonthlyTaskLimit(50);
        testUser.setCreatedAt(Instant.now());
        testUser.setUpdatedAt(Instant.now());
    }

    @Nested
    @DisplayName("Create Group Tests")
    class CreateGroupTests {

        @Test
        @DisplayName("Should create group successfully without users")
        void shouldCreateGroupSuccessfullyWithoutUsers() {
            CreateGroupWithUsersRequest request = new CreateGroupWithUsersRequest();
            request.setName("New Group");
            request.setDescription("New Description");

            when(cuidGenerator.generate()).thenReturn("new-group-id");
            when(groupRepository.existsByNameAndParentGroupIsNull("New Group")).thenReturn(false);
            when(groupRepository.save(any())).thenAnswer(inv -> {
                UserGroup g = inv.getArgument(0);
                g.setCreatedAt(Instant.now());
                g.setUpdatedAt(Instant.now());
                return g;
            });

            CreateGroupWithUsersResponse result = groupService.createGroupWithUsers(request, "admin-user-id");

            assertNotNull(result);
            assertNotNull(result.getGroup());
            assertEquals("new-group-id", result.getGroup().getId());
            assertEquals("New Group", result.getGroup().getName());
            assertNotNull(result.getUsers());
            assertEquals(0, result.getUsers().getAdded());
        }

        @Test
        @DisplayName("Should create group successfully with users")
        void shouldCreateGroupSuccessfullyWithUsers() {
            CreateGroupWithUsersRequest request = new CreateGroupWithUsersRequest();
            request.setName("New Group");
            request.setDescription("New Description");

            List<CreateGroupWithUsersRequest.UserInfo> users = new ArrayList<>();
            CreateGroupWithUsersRequest.UserInfo userInfo = new CreateGroupWithUsersRequest.UserInfo();
            userInfo.setName("John Doe");
            userInfo.setPhone("+1234567890");
            users.add(userInfo);
            request.setUsers(users);

            when(cuidGenerator.generate()).thenReturn("new-group-id", "membership-id");
            when(groupRepository.existsByNameAndParentGroupIsNull("New Group")).thenReturn(false);
            when(groupRepository.save(any())).thenAnswer(inv -> {
                UserGroup g = inv.getArgument(0);
                g.setCreatedAt(Instant.now());
                g.setUpdatedAt(Instant.now());
                return g;
            });
            when(userService.findByPhone("+1234567890")).thenReturn(testUser);
            when(membershipRepository.isUserInGroup(any(), any())).thenReturn(false);
            when(membershipRepository.save(any())).thenAnswer(inv -> {
                UserGroupMembership m = inv.getArgument(0);
                m.setCreatedAt(Instant.now());
                return m;
            });

            CreateGroupWithUsersResponse result = groupService.createGroupWithUsers(request, "admin-user-id");

            assertNotNull(result);
            assertNotNull(result.getGroup());
            assertEquals("new-group-id", result.getGroup().getId());
            assertEquals(1, result.getUsers().getAdded());
        }

        @Test
        @DisplayName("Should throw exception when group name exists at same level")
        void shouldThrowExceptionWhenGroupNameExists() {
            CreateGroupWithUsersRequest request = new CreateGroupWithUsersRequest();
            request.setName("Existing Group");

            when(groupRepository.existsByNameAndParentGroupIsNull("Existing Group")).thenReturn(true);

            assertThrows(ConflictException.class, () -> groupService.createGroupWithUsers(request, "admin-user-id"));
        }

        @Test
        @DisplayName("Should create subgroup with valid parent")
        void shouldCreateSubgroupWithValidParent() {
            CreateGroupWithUsersRequest request = new CreateGroupWithUsersRequest();
            request.setName("Child Group");
            request.setParentGroupId("parent-group-id");

            UserGroup parentGroup = new UserGroup();
            parentGroup.setId("parent-group-id");
            parentGroup.setActive(true);

            when(groupRepository.findById("parent-group-id")).thenReturn(Optional.of(parentGroup));
            when(groupRepository.existsByNameAndParentGroupId("Child Group", "parent-group-id")).thenReturn(false);
            when(cuidGenerator.generate()).thenReturn("child-group-id");
            when(groupRepository.save(any())).thenAnswer(inv -> {
                UserGroup g = inv.getArgument(0);
                g.setCreatedAt(Instant.now());
                g.setUpdatedAt(Instant.now());
                return g;
            });

            CreateGroupWithUsersResponse result = groupService.createGroupWithUsers(request, "admin-user-id");

            assertNotNull(result);
            assertNotNull(result.getGroup());
            assertEquals("child-group-id", result.getGroup().getId());
        }

        @Test
        @DisplayName("Should throw exception when parent group is inactive")
        void shouldThrowExceptionWhenParentGroupInactive() {
            CreateGroupWithUsersRequest request = new CreateGroupWithUsersRequest();
            request.setName("Child Group");
            request.setParentGroupId("parent-group-id");

            UserGroup parentGroup = new UserGroup();
            parentGroup.setId("parent-group-id");
            parentGroup.setActive(false);

            when(groupRepository.findById("parent-group-id")).thenReturn(Optional.of(parentGroup));

            assertThrows(BadRequestException.class, () -> groupService.createGroupWithUsers(request, "admin-user-id"));
        }
    }

    @Nested
    @DisplayName("Add User to Group Tests")
    class AddUserToGroupTests {

        @Test
        @DisplayName("Should add user to group successfully")
        void shouldAddUserToGroupSuccessfully() {
            when(groupRepository.findById("test-group-id")).thenReturn(Optional.of(testGroup));
            when(userRepository.findById("test-user-id")).thenReturn(Optional.of(testUser));
            when(membershipRepository.isUserInGroup("test-user-id", "test-group-id")).thenReturn(false);
            when(cuidGenerator.generate()).thenReturn("membership-id");
            when(membershipRepository.save(any())).thenAnswer(inv -> {
                UserGroupMembership m = inv.getArgument(0);
                m.setCreatedAt(Instant.now());
                return m;
            });

            MembershipResponse result = groupService.addUserToGroup("test-group-id", "test-user-id", "admin-user-id");

            assertNotNull(result);
            assertEquals(GroupMembershipAction.ADDED, result.getAction());
            assertEquals("admin-user-id", result.getPerformedBy());
        }

        @Test
        @DisplayName("Should throw exception when group is inactive")
        void shouldThrowExceptionWhenGroupInactive() {
            testGroup.setActive(false);
            when(groupRepository.findById("test-group-id")).thenReturn(Optional.of(testGroup));

            assertThrows(BadRequestException.class,
                    () -> groupService.addUserToGroup("test-group-id", "test-user-id", "admin-user-id"));
        }

        @Test
        @DisplayName("Should throw exception when user is inactive")
        void shouldThrowExceptionWhenUserInactive() {
            testUser.setStatus(UserStatus.INACTIVE);

            when(groupRepository.findById("test-group-id")).thenReturn(Optional.of(testGroup));
            when(userRepository.findById("test-user-id")).thenReturn(Optional.of(testUser));

            assertThrows(BadRequestException.class,
                    () -> groupService.addUserToGroup("test-group-id", "test-user-id", "admin-user-id"));
        }

        @Test
        @DisplayName("Should throw exception when user already in group")
        void shouldThrowExceptionWhenUserAlreadyInGroup() {
            when(groupRepository.findById("test-group-id")).thenReturn(Optional.of(testGroup));
            when(userRepository.findById("test-user-id")).thenReturn(Optional.of(testUser));
            when(membershipRepository.isUserInGroup("test-user-id", "test-group-id")).thenReturn(true);

            assertThrows(ConflictException.class,
                    () -> groupService.addUserToGroup("test-group-id", "test-user-id", "admin-user-id"));
        }
    }

    @Nested
    @DisplayName("Remove User from Group Tests")
    class RemoveUserFromGroupTests {

        @Test
        @DisplayName("Should remove user from group successfully")
        void shouldRemoveUserFromGroupSuccessfully() {
            when(groupRepository.findById("test-group-id")).thenReturn(Optional.of(testGroup));
            when(userRepository.findById("test-user-id")).thenReturn(Optional.of(testUser));
            when(membershipRepository.isUserInGroup("test-user-id", "test-group-id")).thenReturn(true);
            when(cuidGenerator.generate()).thenReturn("membership-id");
            when(membershipRepository.save(any())).thenAnswer(inv -> {
                UserGroupMembership m = inv.getArgument(0);
                m.setCreatedAt(Instant.now());
                return m;
            });

            MembershipResponse result = groupService.removeUserFromGroup("test-group-id", "test-user-id", "admin-user-id");

            assertNotNull(result);
            assertEquals(GroupMembershipAction.REMOVED, result.getAction());
        }

        @Test
        @DisplayName("Should throw exception when user not in group")
        void shouldThrowExceptionWhenUserNotInGroup() {
            when(groupRepository.findById("test-group-id")).thenReturn(Optional.of(testGroup));
            when(userRepository.findById("test-user-id")).thenReturn(Optional.of(testUser));
            when(membershipRepository.isUserInGroup("test-user-id", "test-group-id")).thenReturn(false);

            assertThrows(BadRequestException.class,
                    () -> groupService.removeUserFromGroup("test-group-id", "test-user-id", "admin-user-id"));
        }
    }

    @Nested
    @DisplayName("Delete Group Tests")
    class DeleteGroupTests {

        @Test
        @DisplayName("Should soft delete group successfully")
        void shouldSoftDeleteGroupSuccessfully() {
            when(groupRepository.findById("test-group-id")).thenReturn(Optional.of(testGroup));
            when(groupRepository.countActiveChildGroups("test-group-id")).thenReturn(0);
            when(groupRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            var result = groupService.deleteGroup("test-group-id");

            assertFalse(result.isActive());
        }

        @Test
        @DisplayName("Should throw exception when group has active children")
        void shouldThrowExceptionWhenGroupHasActiveChildren() {
            when(groupRepository.findById("test-group-id")).thenReturn(Optional.of(testGroup));
            when(groupRepository.countActiveChildGroups("test-group-id")).thenReturn(2);

            assertThrows(BadRequestException.class, () -> groupService.deleteGroup("test-group-id"));
        }

        @Test
        @DisplayName("Should throw exception when group not found")
        void shouldThrowExceptionWhenGroupNotFound() {
            when(groupRepository.findById("non-existent")).thenReturn(Optional.empty());

            assertThrows(NotFoundException.class, () -> groupService.deleteGroup("non-existent"));
        }
    }
}
