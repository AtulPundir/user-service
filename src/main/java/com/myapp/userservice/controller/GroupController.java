package com.myapp.userservice.controller;

import com.myapp.userservice.dto.request.*;
import com.myapp.userservice.dto.response.*;
import com.myapp.userservice.security.SecurityUtils;
import com.myapp.userservice.service.GroupService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/groups")
@Tag(name = "Groups", description = "Group management endpoints")
public class GroupController {

    private final GroupService groupService;
    private final SecurityUtils securityUtils;

    public GroupController(GroupService groupService, SecurityUtils securityUtils) {
        this.groupService = groupService;
        this.securityUtils = securityUtils;
    }

    @PostMapping
    @Operation(summary = "Create a new group, optionally with initial users")
    public ResponseEntity<ApiResponse<CreateGroupWithUsersResponse>> createGroup(
            @Valid @RequestBody CreateGroupWithUsersRequest request) {
        String performedBy = securityUtils.getCurrentUserId();
        CreateGroupWithUsersResponse result = groupService.createGroupWithUsers(request, performedBy);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(result, "Group created successfully"));
    }

    @GetMapping
    @Operation(summary = "List all groups with pagination")
    public ResponseEntity<ApiResponse<List<GroupResponse>>> listGroups(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(required = false) Boolean isActive,
            @RequestParam(required = false) String parentGroupId) {
        return ResponseEntity.ok(groupService.listGroups(page, limit, isActive, parentGroupId));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get group by ID")
    public ResponseEntity<ApiResponse<GroupResponse>> getGroupById(@PathVariable String id) {
        GroupResponse group = groupService.getGroupById(id);
        return ResponseEntity.ok(ApiResponse.success(group));
    }

    @PatchMapping("/{id}")
    @Operation(summary = "Update group")
    public ResponseEntity<ApiResponse<GroupResponse>> updateGroup(
            @PathVariable String id,
            @Valid @RequestBody UpdateGroupRequest request) {
        GroupResponse group = groupService.updateGroup(id, request);
        return ResponseEntity.ok(ApiResponse.success(group, "Group updated successfully"));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete group (soft delete)")
    public ResponseEntity<ApiResponse<GroupResponse>> deleteGroup(@PathVariable String id) {
        GroupResponse group = groupService.deleteGroup(id);
        return ResponseEntity.ok(ApiResponse.success(group, "Group deleted successfully"));
    }

    @PostMapping("/{id}/members")
    @Operation(summary = "Add user to group")
    public ResponseEntity<ApiResponse<MembershipResponse>> addUserToGroup(
            @PathVariable String id,
            @Valid @RequestBody AddUserToGroupRequest request) {
        String performedBy = securityUtils.getCurrentUserId();
        MembershipResponse membership = groupService.addUserToGroup(id, request.getUserId(), performedBy);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(membership, "User added to group successfully"));
    }

    @PostMapping("/{id}/members/bulk")
    @Operation(summary = "Add multiple users to group")
    public ResponseEntity<ApiResponse<BulkAddUsersResponse>> bulkAddUsers(
            @PathVariable String id,
            @Valid @RequestBody BulkAddUsersRequest request) {
        String performedBy = securityUtils.getCurrentUserId();
        BulkAddUsersResponse result = groupService.bulkAddUsers(id, request, performedBy);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(result, "Users processed successfully"));
    }

    @GetMapping("/{id}/members")
    @Operation(summary = "Get group members")
    public ResponseEntity<ApiResponse<List<UserResponse>>> getGroupMembers(@PathVariable String id) {
        List<UserResponse> members = groupService.getGroupMembers(id);
        return ResponseEntity.ok(ApiResponse.success(members));
    }

    @DeleteMapping("/{id}/members/{userId}")
    @Operation(summary = "Remove user from group")
    public ResponseEntity<ApiResponse<MembershipResponse>> removeUserFromGroup(
            @PathVariable String id,
            @PathVariable String userId) {
        String performedBy = securityUtils.getCurrentUserId();
        MembershipResponse membership = groupService.removeUserFromGroup(id, userId, performedBy);
        return ResponseEntity.ok(ApiResponse.success(membership, "User removed from group successfully"));
    }

    @GetMapping("/{id}/history")
    @Operation(summary = "Get group membership history")
    public ResponseEntity<ApiResponse<List<MembershipResponse>>> getGroupHistory(
            @PathVariable String id,
            @RequestParam(required = false) String userId) {
        List<MembershipResponse> history = groupService.getGroupHistory(id, userId);
        return ResponseEntity.ok(ApiResponse.success(history));
    }
}
