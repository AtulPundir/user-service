package com.myapp.identityservice.controller;

import com.myapp.identityservice.dto.request.CreateUserRequest;
import com.myapp.identityservice.dto.request.OnboardUserRequest;
import com.myapp.identityservice.dto.response.ApiResponse;
import com.myapp.identityservice.dto.response.OnboardUserResponse;
import com.myapp.identityservice.dto.response.UserResponse;
import com.myapp.identityservice.service.InvitationService;
import com.myapp.identityservice.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Internal API endpoints for service-to-service communication.
 * These endpoints are called by other services (e.g., Auth Service) and should be
 * secured with service-to-service authentication in production.
 */
@RestController
@RequestMapping("/internal")
@Tag(name = "Internal", description = "Internal service-to-service endpoints")
public class InternalController {

    private final InvitationService invitationService;
    private final UserService userService;

    public InternalController(InvitationService invitationService, UserService userService) {
        this.invitationService = invitationService;
        this.userService = userService;
    }

    /**
     * Create a new user. This endpoint is called by Auth Service after user registration.
     * For users who may have pending invitations, use /users/onboard instead.
     *
     * @deprecated Prefer using POST /internal/users/onboard which also resolves invitations.
     */
    @PostMapping("/users")
    @Operation(summary = "Create a new user (called by Auth Service)")
    public ResponseEntity<ApiResponse<UserResponse>> createUser(@Valid @RequestBody CreateUserRequest request) {
        UserResponse user = userService.createUser(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(user, "User created successfully"));
    }

    /**
     * Onboard a new user after they complete signup via Auth Service.
     * This endpoint:
     * 1. Creates the user record (if not already exists)
     * 2. Resolves any pending invitations matching the user's email or phone
     * 3. Adds the user to invited groups
     *
     * This operation is idempotent - calling it multiple times with the same
     * authUserId will return the same result.
     */
    @PostMapping("/users/onboard")
    @Operation(summary = "Onboard a newly signed-up user and resolve pending invitations")
    public ResponseEntity<ApiResponse<OnboardUserResponse>> onboardUser(
            @Valid @RequestBody OnboardUserRequest request) {

        OnboardUserResponse response = invitationService.onboardUser(request);

        String message = response.isUserCreated()
                ? "User created and invitations resolved"
                : "User already exists, invitations resolved";

        return ResponseEntity.status(response.isUserCreated() ? HttpStatus.CREATED : HttpStatus.OK)
                .body(ApiResponse.success(response, message));
    }

    /**
     * Expire all overdue invitations.
     * This can be called by a scheduled job or manually by an admin.
     */
    @PostMapping("/invitations/expire-overdue")
    @Operation(summary = "Expire all overdue invitations")
    public ResponseEntity<ApiResponse<Integer>> expireOverdueInvitations() {
        int expired = invitationService.expireOverdueInvitations();
        return ResponseEntity.ok(ApiResponse.success(expired, expired + " invitations expired"));
    }
}
