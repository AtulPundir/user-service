package com.myapp.identityservice.controller;

import com.myapp.identityservice.dto.request.BatchResolveRequest;
import com.myapp.identityservice.dto.request.ResolveOrCreateUserRequest;
import com.myapp.identityservice.dto.response.ApiResponse;
import com.myapp.identityservice.dto.response.BatchResolveResponse;
import com.myapp.identityservice.dto.response.ResolveOrCreateUserResponse;
import com.myapp.identityservice.security.SecurityUtils;
import com.myapp.identityservice.service.BatchResolveService;
import com.myapp.identityservice.service.PlaceholderUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users")
@Tag(name = "Placeholder Users", description = "Resolve or create placeholder users for unregistered contacts")
public class PlaceholderUserController {

    private final PlaceholderUserService placeholderUserService;
    private final BatchResolveService batchResolveService;
    private final SecurityUtils securityUtils;

    public PlaceholderUserController(PlaceholderUserService placeholderUserService,
                                      BatchResolveService batchResolveService,
                                      SecurityUtils securityUtils) {
        this.placeholderUserService = placeholderUserService;
        this.batchResolveService = batchResolveService;
        this.securityUtils = securityUtils;
    }

    @PostMapping("/resolve-or-create")
    @Operation(summary = "Resolve existing user or create a placeholder for a phone/email")
    public ResponseEntity<ApiResponse<ResolveOrCreateUserResponse>> resolveOrCreate(
            @Valid @RequestBody ResolveOrCreateUserRequest request) {
        String currentUserId = securityUtils.getCurrentUserId();
        ResolveOrCreateUserResponse response = placeholderUserService.resolveOrCreate(request, currentUserId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/batch-resolve")
    @Operation(summary = "Batch resolve user display information with viewer-specific aliases")
    public ResponseEntity<ApiResponse<BatchResolveResponse>> batchResolve(
            @Valid @RequestBody BatchResolveRequest request) {
        String viewerUserId = securityUtils.getCurrentUserId();
        BatchResolveResponse response = batchResolveService.batchResolve(request.getUserIds(), viewerUserId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
