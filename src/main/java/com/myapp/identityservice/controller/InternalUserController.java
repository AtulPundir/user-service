package com.myapp.identityservice.controller;

import com.myapp.identityservice.domain.User;
import com.myapp.identityservice.dto.request.BatchResolveRequest;
import com.myapp.identityservice.dto.response.ApiResponse;
import com.myapp.identityservice.dto.response.BatchResolveResponse;
import com.myapp.identityservice.dto.response.UserDisplayInfo;
import com.myapp.identityservice.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Internal service-to-service endpoint for batch-resolving user display info.
 * Authenticated via API key (X-API-Key header), no JWT required.
 *
 * <p>Unlike the user-facing {@code /api/v1/users/batch-resolve} which includes
 * viewer-specific aliases, this endpoint returns only canonical names and
 * verification status — sufficient for backend reconciliation jobs.</p>
 */
@RestController
@RequestMapping("/internal/users")
@Tag(name = "Internal Users", description = "Service-to-service user resolution endpoints")
public class InternalUserController {

    private static final Logger logger = LoggerFactory.getLogger(InternalUserController.class);

    private final UserRepository userRepository;

    public InternalUserController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @PostMapping("/batch-resolve")
    @Operation(summary = "Batch resolve user display info (internal, no viewer context)")
    public ResponseEntity<ApiResponse<BatchResolveResponse>> batchResolve(
            @Valid @RequestBody BatchResolveRequest request) {

        List<User> users = userRepository.findByIdIn(request.getUserIds());

        Map<String, UserDisplayInfo> userMap = users.stream()
                .collect(Collectors.toMap(
                        User::getId,
                        user -> new UserDisplayInfo(
                                user.getName(),
                                user.isVerified(),
                                null  // no viewer context for internal calls
                        )
                ));

        return ResponseEntity.ok(ApiResponse.success(new BatchResolveResponse(userMap)));
    }

    /**
     * Look up a user by phone number or email.
     * Used by wow-service to resolve counterparty userId from contact info.
     *
     * @param phone Phone number (optional, normalized with + prefix)
     * @param email Email address (optional)
     * @return User info if found, 404 if not found
     */
    @GetMapping("/lookup")
    @Operation(summary = "Look up user by phone or email (internal)")
    public ResponseEntity<ApiResponse<UserLookupResponse>> lookupUser(
            @RequestParam(required = false) String phone,
            @RequestParam(required = false) String email) {

        if (phone == null && email == null) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("MISSING_IDENTIFIER", "Either phone or email is required"));
        }

        // Try phone first if provided
        if (phone != null) {
            String normalizedPhone = normalizePhone(phone);
            logger.info("[InternalUserController] Looking up user - received phone: '{}', normalized: '{}'", phone, normalizedPhone);
            return userRepository.findByPhone(normalizedPhone)
                    .map(user -> {
                        logger.info("[InternalUserController] User found: id={}, name={}", user.getId(), user.getName());
                        return ResponseEntity.ok(ApiResponse.success(
                            new UserLookupResponse(user.getId(), user.getName(), user.isVerified())));
                    })
                    .orElseGet(() -> {
                        logger.info("[InternalUserController] No user found for phone: '{}'", normalizedPhone);
                        // If phone lookup fails and email is provided, try email
                        if (email != null) {
                            return lookupByEmail(email);
                        }
                        return ResponseEntity.status(404)
                                .body(ApiResponse.error("USER_NOT_FOUND", "No user found with the provided phone"));
                    });
        }

        // Try email
        return lookupByEmail(email);
    }

    private ResponseEntity<ApiResponse<UserLookupResponse>> lookupByEmail(String email) {
        return userRepository.findByEmail(email.toLowerCase())
                .map(user -> ResponseEntity.ok(ApiResponse.success(
                        new UserLookupResponse(user.getId(), user.getName(), user.isVerified()))))
                .orElse(ResponseEntity.status(404)
                        .body(ApiResponse.error("USER_NOT_FOUND", "No user found with the provided email")));
    }

    private String normalizePhone(String phone) {
        if (phone == null) return null;
        String normalized = phone.replaceAll("[^0-9+]", "");
        if (!normalized.startsWith("+")) {
            normalized = "+" + normalized;
        }
        return normalized;
    }

    /**
     * Response DTO for user lookup.
     */
    public record UserLookupResponse(String userId, String name, boolean verified) {}
}
