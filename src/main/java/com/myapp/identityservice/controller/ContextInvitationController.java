package com.myapp.identityservice.controller;

import com.myapp.identityservice.dto.request.CreateContextInvitationRequest;
import com.myapp.identityservice.dto.response.ApiResponse;
import com.myapp.identityservice.dto.response.ContextInvitationResponse;
import com.myapp.identityservice.security.SecurityUtils;
import com.myapp.identityservice.service.ContextInvitationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/invitations")
@Tag(name = "Context Invitations", description = "Generic invitations for trips, collaborations, agreements, expense groups")
public class ContextInvitationController {

    private final ContextInvitationService invitationService;
    private final SecurityUtils securityUtils;

    public ContextInvitationController(ContextInvitationService invitationService,
                                        SecurityUtils securityUtils) {
        this.invitationService = invitationService;
        this.securityUtils = securityUtils;
    }

    @PostMapping
    @Operation(summary = "Create a context-aware invitation")
    public ResponseEntity<ApiResponse<ContextInvitationResponse>> createInvitation(
            @Valid @RequestBody CreateContextInvitationRequest request) {
        String userId = securityUtils.getCurrentUserId();
        ContextInvitationResponse response = invitationService.createInvitation(userId, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/my")
    @Operation(summary = "Get my pending invitations")
    public ResponseEntity<ApiResponse<List<ContextInvitationResponse>>> getMyInvitations() {
        String userId = securityUtils.getCurrentUserId();
        List<ContextInvitationResponse> invitations = invitationService.getMyPendingInvitations(userId);
        return ResponseEntity.ok(ApiResponse.success(invitations));
    }

    @PostMapping("/{id}/accept")
    @Operation(summary = "Accept an invitation")
    public ResponseEntity<ApiResponse<ContextInvitationResponse>> acceptInvitation(@PathVariable String id) {
        String userId = securityUtils.getCurrentUserId();
        ContextInvitationResponse response = invitationService.acceptInvitation(id, userId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/{id}/reject")
    @Operation(summary = "Reject an invitation")
    public ResponseEntity<ApiResponse<ContextInvitationResponse>> rejectInvitation(@PathVariable String id) {
        String userId = securityUtils.getCurrentUserId();
        ContextInvitationResponse response = invitationService.rejectInvitation(id, userId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
