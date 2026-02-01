package com.myapp.identityservice.controller;

import com.myapp.identityservice.domain.UserStatus;
import com.myapp.identityservice.dto.request.UpdateUserRequest;
import com.myapp.identityservice.dto.request.UserFilterRequest;
import com.myapp.identityservice.dto.response.ApiResponse;
import com.myapp.identityservice.dto.response.GroupResponse;
import com.myapp.identityservice.dto.response.UserResponse;
import com.myapp.identityservice.security.SecurityUtils;
import com.myapp.identityservice.service.GroupService;
import com.myapp.identityservice.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/users")
@Tag(name = "Users", description = "User management endpoints")
public class UserController {

    private final UserService userService;
    private final GroupService groupService;
    private final SecurityUtils securityUtils;

    public UserController(UserService userService, GroupService groupService, SecurityUtils securityUtils) {
        this.userService = userService;
        this.groupService = groupService;
        this.securityUtils = securityUtils;
    }

    @GetMapping("/me")
    @Operation(summary = "Get current user profile")
    public ResponseEntity<ApiResponse<UserResponse>> getCurrentUser() {
        String userId = securityUtils.getCurrentUserId();
        UserResponse user = userService.getCurrentUser(userId);
        return ResponseEntity.ok(ApiResponse.success(user));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "List all users with pagination (Admin only)")
    public ResponseEntity<ApiResponse<List<UserResponse>>> listUsers(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(required = false) UserStatus status,
            @RequestParam(required = false) Boolean verified) {
        UserFilterRequest filter = new UserFilterRequest();
        filter.setStatus(status);
        filter.setVerified(verified);
        return ResponseEntity.ok(userService.listUsers(page, limit, filter));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get user by ID")
    public ResponseEntity<ApiResponse<UserResponse>> getUserById(@PathVariable String id) {
        UserResponse user = userService.getUserById(id);
        return ResponseEntity.ok(ApiResponse.success(user));
    }

    @PatchMapping("/{id}")
    @Operation(summary = "Update user")
    public ResponseEntity<ApiResponse<UserResponse>> updateUser(
            @PathVariable String id,
            @Valid @RequestBody UpdateUserRequest request) {
        UserResponse user = userService.updateUser(id, request);
        return ResponseEntity.ok(ApiResponse.success(user, "User updated successfully"));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete user (soft delete)")
    public ResponseEntity<ApiResponse<UserResponse>> deleteUser(@PathVariable String id) {
        UserResponse user = userService.deleteUser(id);
        return ResponseEntity.ok(ApiResponse.success(user, "User deleted successfully"));
    }

    @GetMapping("/{id}/groups")
    @Operation(summary = "Get user's groups")
    public ResponseEntity<ApiResponse<List<GroupResponse>>> getUserGroups(@PathVariable String id) {
        List<GroupResponse> groups = groupService.getUserGroups(id);
        return ResponseEntity.ok(ApiResponse.success(groups));
    }
}
