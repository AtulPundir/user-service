package com.myapp.userservice.service;

import com.myapp.userservice.domain.*;
import com.myapp.userservice.dto.request.BulkAddUsersRequest;
import com.myapp.userservice.dto.request.CreateGroupWithUsersRequest;
import com.myapp.userservice.dto.request.UpdateGroupRequest;
import com.myapp.userservice.dto.response.*;
import com.myapp.userservice.exception.BadRequestException;
import com.myapp.userservice.exception.ConflictException;
import com.myapp.userservice.exception.NotFoundException;
import com.myapp.userservice.repository.UserGroupMembershipRepository;
import com.myapp.userservice.repository.UserGroupRepository;
import com.myapp.userservice.repository.UserRepository;
import com.myapp.userservice.util.CuidGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class GroupService {

    private static final Logger logger = LoggerFactory.getLogger(GroupService.class);

    private final UserGroupRepository groupRepository;
    private final UserGroupMembershipRepository membershipRepository;
    private final UserRepository userRepository;
    private final UserService userService;
    private final CuidGenerator cuidGenerator;

    public GroupService(UserGroupRepository groupRepository,
                       UserGroupMembershipRepository membershipRepository,
                       UserRepository userRepository,
                       UserService userService,
                       CuidGenerator cuidGenerator) {
        this.groupRepository = groupRepository;
        this.membershipRepository = membershipRepository;
        this.userRepository = userRepository;
        this.userService = userService;
        this.cuidGenerator = cuidGenerator;
    }

    @Transactional
    public CreateGroupWithUsersResponse createGroupWithUsers(CreateGroupWithUsersRequest request, String performedBy) {
        // Validate parent group if provided
        UserGroup parentGroup = null;
        if (request.getParentGroupId() != null) {
            parentGroup = groupRepository.findById(request.getParentGroupId())
                    .orElseThrow(NotFoundException::parentGroup);

            if (!parentGroup.isActive()) {
                throw new BadRequestException("Parent group is not active");
            }
        }

        // Note: We allow groups with the same name as long as they have different members.
        // Duplicate detection (same name + same members) is performed when members are added.

        // Create the group
        UserGroup group = new UserGroup();
        group.setId(cuidGenerator.generate());
        group.setName(request.getName());
        group.setDescription(request.getDescription());
        group.setParentGroup(parentGroup);
        group.setActive(true);

        UserGroup savedGroup = groupRepository.save(group);
        logger.info("Group created: id={}, name={}, createdBy={}", savedGroup.getId(), savedGroup.getName(), performedBy);

        GroupResponse groupResponse = GroupResponse.fromEntity(savedGroup);

        // Process users if provided
        BulkAddUsersResponse usersResponse = new BulkAddUsersResponse();

        if (request.getUsers() != null && !request.getUsers().isEmpty()) {
            // Collect user IDs that will be added to this group
            Set<String> newMemberIds = new HashSet<>();
            for (CreateGroupWithUsersRequest.UserInfo userInfo : request.getUsers()) {
                User user = userService.findByPhone(userInfo.getPhone());
                if (user != null) {
                    newMemberIds.add(user.getId());
                }
            }

            // Check for duplicate group before adding members
            if (!newMemberIds.isEmpty()) {
                checkForDuplicateGroup(savedGroup.getId(), savedGroup.getName(), newMemberIds);
            }

            for (CreateGroupWithUsersRequest.UserInfo userInfo : request.getUsers()) {
                processUserForGroup(savedGroup, userInfo.getName(), userInfo.getPhone(), performedBy, usersResponse);
            }
        }

        return new CreateGroupWithUsersResponse(groupResponse, usersResponse);
    }

    @Transactional(readOnly = true)
    public ApiResponse<List<GroupResponse>> listGroups(int page, int limit, Boolean isActive, String parentGroupId) {
        Pageable pageable = PageRequest.of(page - 1, limit);

        Page<UserGroup> groupPage;
        if (isActive != null && parentGroupId != null) {
            groupPage = groupRepository.findByIsActiveAndParentGroupIdOrderByCreatedAtDesc(isActive, parentGroupId, pageable);
        } else if (isActive != null) {
            groupPage = groupRepository.findByIsActiveOrderByCreatedAtDesc(isActive, pageable);
        } else if (parentGroupId != null) {
            groupPage = groupRepository.findByParentGroupIdOrderByCreatedAtDesc(parentGroupId, pageable);
        } else {
            groupPage = groupRepository.findAllOrderByCreatedAtDesc(pageable);
        }

        List<GroupResponse> groups = groupPage.getContent()
                .stream()
                .map(group -> {
                    int childCount = groupRepository.countChildGroups(group.getId());
                    return GroupResponse.fromEntityWithChildCount(group, childCount);
                })
                .collect(Collectors.toList());

        PaginationInfo pagination = new PaginationInfo(page, limit, groupPage.getTotalElements());

        return ApiResponse.success(groups, pagination);
    }

    @Transactional(readOnly = true)
    public GroupResponse getGroupById(String id) {
        UserGroup group = groupRepository.findByIdWithParentAndChildren(id)
                .orElseThrow(() -> NotFoundException.group(id));

        GroupResponse response = GroupResponse.fromEntityWithParent(group);

        // Get child groups
        List<GroupResponse> childGroups = group.getChildGroups()
                .stream()
                .map(GroupResponse::fromEntity)
                .collect(Collectors.toList());
        response.setChildGroups(childGroups);

        // Get all memberships
        List<UserGroupMembership> memberships = membershipRepository.findByGroupIdWithUserOrderByCreatedAtDesc(id);
        List<MembershipResponse> membershipResponses = memberships.stream()
                .map(MembershipResponse::fromEntityWithUser)
                .collect(Collectors.toList());
        response.setMemberships(membershipResponses);

        // Calculate current members
        List<UserResponse> currentMembers = getCurrentMembers(id);
        response.setCurrentMembers(currentMembers);

        return response;
    }

    @Transactional
    public GroupResponse updateGroup(String id, UpdateGroupRequest request) {
        UserGroup group = groupRepository.findById(id)
                .orElseThrow(() -> NotFoundException.group(id));

        if (request.getName() != null) {
            // Check for duplicate name at same level (excluding current group)
            String parentGroupId = group.getParentGroupId();
            if (!request.getName().equals(group.getName()) && existsByNameAtLevel(request.getName(), parentGroupId)) {
                throw ConflictException.groupNameExists();
            }
            group.setName(request.getName());
        }

        if (request.getDescription() != null) {
            group.setDescription(request.getDescription());
        }

        if (request.getIsActive() != null) {
            group.setActive(request.getIsActive());
        }

        UserGroup savedGroup = groupRepository.save(group);
        logger.info("Group updated: id={}", savedGroup.getId());

        return GroupResponse.fromEntity(savedGroup);
    }

    @Transactional
    public GroupResponse deleteGroup(String id) {
        UserGroup group = groupRepository.findById(id)
                .orElseThrow(() -> NotFoundException.group(id));

        // Check for active child groups
        int activeChildCount = groupRepository.countActiveChildGroups(id);
        if (activeChildCount > 0) {
            throw new BadRequestException("Cannot delete group with active child groups");
        }

        group.setActive(false);
        UserGroup savedGroup = groupRepository.save(group);
        logger.info("Group deleted (soft): id={}", savedGroup.getId());

        return GroupResponse.fromEntity(savedGroup);
    }

    @Transactional
    public MembershipResponse addUserToGroup(String groupId, String userId, String performedBy) {
        UserGroup group = groupRepository.findById(groupId)
                .orElseThrow(() -> NotFoundException.group(groupId));

        if (!group.isActive()) {
            throw new BadRequestException("Group is not active");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> NotFoundException.user(userId));

        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new BadRequestException("User is not active");
        }

        // Check if user is already in group
        if (membershipRepository.isUserInGroup(userId, groupId)) {
            throw ConflictException.userAlreadyInGroup();
        }

        // Check for duplicate group (same name + same members) after adding this user
        Set<String> currentMemberIds = getCurrentMemberIds(groupId);
        currentMemberIds.add(userId);
        checkForDuplicateGroup(groupId, group.getName(), currentMemberIds);

        UserGroupMembership membership = createMembership(group, user, GroupMembershipAction.ADDED, performedBy);
        logger.info("User added to group: userId={}, groupId={}, performedBy={}", userId, groupId, performedBy);

        return MembershipResponse.fromEntity(membership);
    }

    @Transactional
    public BulkAddUsersResponse bulkAddUsers(String groupId, BulkAddUsersRequest request, String performedBy) {
        UserGroup group = groupRepository.findById(groupId)
                .orElseThrow(() -> NotFoundException.group(groupId));

        if (!group.isActive()) {
            throw new BadRequestException("Group is not active");
        }

        // Get current members and calculate what the new member set will be
        Set<String> projectedMemberIds = getCurrentMemberIds(groupId);

        // Collect all user IDs that will be added
        for (BulkAddUsersRequest.UserInfo userInfo : request.getUsers()) {
            User user = userService.findByPhone(userInfo.getPhone());
            if (user != null && user.getStatus() == UserStatus.ACTIVE
                    && !membershipRepository.isUserInGroup(user.getId(), groupId)) {
                projectedMemberIds.add(user.getId());
            }
        }

        // Check for duplicate group before adding any members
        checkForDuplicateGroup(groupId, group.getName(), projectedMemberIds);

        BulkAddUsersResponse response = new BulkAddUsersResponse();

        for (BulkAddUsersRequest.UserInfo userInfo : request.getUsers()) {
            processUserForGroup(group, userInfo.getName(), userInfo.getPhone(), performedBy, response);
        }

        return response;
    }

    @Transactional(readOnly = true)
    public List<UserResponse> getGroupMembers(String groupId) {
        if (!groupRepository.existsById(groupId)) {
            throw NotFoundException.group(groupId);
        }

        return getCurrentMembers(groupId);
    }

    @Transactional
    public MembershipResponse removeUserFromGroup(String groupId, String userId, String performedBy) {
        UserGroup group = groupRepository.findById(groupId)
                .orElseThrow(() -> NotFoundException.group(groupId));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> NotFoundException.user(userId));

        // Check if user is in group
        if (!membershipRepository.isUserInGroup(userId, groupId)) {
            throw new BadRequestException("User is not in this group");
        }

        UserGroupMembership membership = createMembership(group, user, GroupMembershipAction.REMOVED, performedBy);
        logger.info("User removed from group: userId={}, groupId={}, performedBy={}", userId, groupId, performedBy);

        return MembershipResponse.fromEntity(membership);
    }

    @Transactional(readOnly = true)
    public List<MembershipResponse> getGroupHistory(String groupId, String userId) {
        if (!groupRepository.existsById(groupId)) {
            throw NotFoundException.group(groupId);
        }

        List<UserGroupMembership> memberships;
        if (userId != null) {
            memberships = membershipRepository.findByGroupIdAndUserIdWithUserOrderByCreatedAtDesc(groupId, userId);
        } else {
            memberships = membershipRepository.findByGroupIdWithUserOrderByCreatedAtDesc(groupId);
        }

        return memberships.stream()
                .map(MembershipResponse::fromEntityWithUser)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<GroupResponse> getUserGroups(String userId) {
        if (!userRepository.existsById(userId)) {
            throw NotFoundException.user(userId);
        }

        List<UserGroupMembership> memberships = membershipRepository.findByUserIdWithGroupOrderByCreatedAtDesc(userId);

        // Determine current group memberships
        Map<String, GroupMembershipAction> latestActions = new LinkedHashMap<>();
        for (UserGroupMembership membership : memberships) {
            String gid = membership.getGroup().getId();
            if (!latestActions.containsKey(gid)) {
                latestActions.put(gid, membership.getAction());
            }
        }

        // Return only groups where user is currently a member (ADDED) and group is active
        return latestActions.entrySet().stream()
                .filter(entry -> entry.getValue() == GroupMembershipAction.ADDED)
                .map(entry -> groupRepository.findById(entry.getKey()).orElse(null))
                .filter(group -> group != null && group.isActive())
                .map(GroupResponse::fromEntity)
                .collect(Collectors.toList());
    }

    private void processUserForGroup(UserGroup group, String name, String phone,
                                    String performedBy, BulkAddUsersResponse response) {
        try {
            // Find or create user
            User user = userService.findByPhone(phone);
            boolean wasCreated = false;

            if (user == null) {
                user = userService.createUnverifiedUser(name, phone);
                wasCreated = true;
            }

            // Check if user is active
            if (user.getStatus() != UserStatus.ACTIVE) {
                response.addAlreadyMember(UserResponse.fromEntity(user), "User is not active");
                return;
            }

            // Check if already in group
            if (membershipRepository.isUserInGroup(user.getId(), group.getId())) {
                response.addAlreadyMember(UserResponse.fromEntity(user), "User is already a member of this group");
                return;
            }

            // Add to group
            UserGroupMembership membership = createMembership(group, user, GroupMembershipAction.ADDED, performedBy);

            if (wasCreated) {
                response.addCreatedUser(UserResponse.fromEntity(user), user.isVerified());
            }

            response.addAddedUser(UserResponse.fromEntity(user), MembershipResponse.fromEntity(membership));

        } catch (Exception e) {
            logger.warn("Error processing user for group: phone={}, error={}", phone, e.getMessage());
        }
    }

    private UserGroupMembership createMembership(UserGroup group, User user,
                                                 GroupMembershipAction action, String performedBy) {
        UserGroupMembership membership = new UserGroupMembership();
        membership.setId(cuidGenerator.generate());
        membership.setGroup(group);
        membership.setUser(user);
        membership.setAction(action);
        membership.setPerformedBy(performedBy);

        return membershipRepository.save(membership);
    }

    private List<UserResponse> getCurrentMembers(String groupId) {
        List<UserGroupMembership> memberships = membershipRepository.findByGroupIdWithUserOrderByCreatedAtDesc(groupId);

        // Track the latest action per user
        Map<String, UserGroupMembership> latestMembershipByUser = new LinkedHashMap<>();
        for (UserGroupMembership membership : memberships) {
            String uid = membership.getUser().getId();
            if (!latestMembershipByUser.containsKey(uid)) {
                latestMembershipByUser.put(uid, membership);
            }
        }

        // Return users where latest action is ADDED and user is active
        return latestMembershipByUser.values().stream()
                .filter(m -> m.getAction() == GroupMembershipAction.ADDED)
                .map(UserGroupMembership::getUser)
                .filter(u -> u.getStatus() == UserStatus.ACTIVE)
                .map(UserResponse::fromEntity)
                .collect(Collectors.toList());
    }

    private boolean existsByNameAtLevel(String name, String parentGroupId) {
        if (parentGroupId == null) {
            return groupRepository.existsByNameAndParentGroupIsNull(name);
        }
        return groupRepository.existsByNameAndParentGroupId(name, parentGroupId);
    }

    /**
     * Get the set of current member IDs for a group.
     * Only includes users whose latest action is ADDED and are currently ACTIVE.
     */
    private Set<String> getCurrentMemberIds(String groupId) {
        List<UserGroupMembership> memberships = membershipRepository.findByGroupIdWithUserOrderByCreatedAtDesc(groupId);

        // Track the latest action per user
        Map<String, UserGroupMembership> latestMembershipByUser = new LinkedHashMap<>();
        for (UserGroupMembership membership : memberships) {
            String uid = membership.getUser().getId();
            if (!latestMembershipByUser.containsKey(uid)) {
                latestMembershipByUser.put(uid, membership);
            }
        }

        // Return user IDs where latest action is ADDED and user is active
        return latestMembershipByUser.values().stream()
                .filter(m -> m.getAction() == GroupMembershipAction.ADDED)
                .map(UserGroupMembership::getUser)
                .filter(u -> u.getStatus() == UserStatus.ACTIVE)
                .map(User::getId)
                .collect(Collectors.toSet());
    }

    /**
     * Check if adding members to a group would create a duplicate group.
     * A duplicate is defined as another group with:
     * - Same name (case insensitive)
     * - Exactly the same set of members
     *
     * @param currentGroupId The ID of the group being modified
     * @param groupName The name of the group
     * @param projectedMemberIds The set of member IDs after the operation
     * @throws ConflictException if a duplicate group is found
     */
    private void checkForDuplicateGroup(String currentGroupId, String groupName, Set<String> projectedMemberIds) {
        // Find all other active groups with the same name (case insensitive)
        List<UserGroup> sameNameGroups = groupRepository.findByNameIgnoreCaseAndIsActiveTrueAndIdNot(
                groupName, currentGroupId);

        if (sameNameGroups.isEmpty()) {
            return;
        }

        // Check each group for exact member match
        for (UserGroup otherGroup : sameNameGroups) {
            Set<String> otherGroupMemberIds = getCurrentMemberIds(otherGroup.getId());

            // Check for exact match
            if (projectedMemberIds.equals(otherGroupMemberIds)) {
                logger.warn("Duplicate group detected: groupId={}, duplicateOf={}, name={}",
                        currentGroupId, otherGroup.getId(), groupName);
                throw ConflictException.duplicateGroup(otherGroup.getId(), otherGroup.getName());
            }
        }
    }
}
