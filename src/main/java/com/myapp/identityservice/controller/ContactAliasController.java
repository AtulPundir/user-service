package com.myapp.identityservice.controller;

import com.myapp.identityservice.dto.request.SetContactAliasRequest;
import com.myapp.identityservice.dto.response.ApiResponse;
import com.myapp.identityservice.dto.response.ContactAliasResponse;
import com.myapp.identityservice.security.SecurityUtils;
import com.myapp.identityservice.service.ContactAliasService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/contacts")
@Tag(name = "Contact Aliases", description = "Manage viewer-specific names for contacts")
public class ContactAliasController {

    private final ContactAliasService contactAliasService;
    private final SecurityUtils securityUtils;

    public ContactAliasController(ContactAliasService contactAliasService,
                                   SecurityUtils securityUtils) {
        this.contactAliasService = contactAliasService;
        this.securityUtils = securityUtils;
    }

    @PostMapping("/alias")
    @Operation(summary = "Set or update a contact alias")
    public ResponseEntity<ApiResponse<ContactAliasResponse>> setAlias(
            @Valid @RequestBody SetContactAliasRequest request) {
        String userId = securityUtils.getCurrentUserId();
        ContactAliasResponse response = contactAliasService.setAlias(userId, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/aliases")
    @Operation(summary = "Get all contact aliases for the current user")
    public ResponseEntity<ApiResponse<List<ContactAliasResponse>>> getAliases() {
        String userId = securityUtils.getCurrentUserId();
        List<ContactAliasResponse> aliases = contactAliasService.getAliases(userId);
        return ResponseEntity.ok(ApiResponse.success(aliases));
    }
}
