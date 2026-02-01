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
}
