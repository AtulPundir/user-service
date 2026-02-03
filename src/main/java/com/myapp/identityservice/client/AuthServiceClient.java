package com.myapp.identityservice.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;

/**
 * Client for calling auth-service internal endpoints.
 * Used to resolve or create users so that auth-service is the single source
 * of truth for user IDs.
 */
@Component
public class AuthServiceClient {

    private static final Logger logger = LoggerFactory.getLogger(AuthServiceClient.class);

    private final RestClient restClient;

    public AuthServiceClient(@Value("${app.auth-service.base-url}") String baseUrl) {
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .build();
    }

    /**
     * Resolve an existing user or create a placeholder in auth-service.
     * Returns the user ID that identity-service MUST use.
     *
     * @param identityKey Phone or email
     * @param identityType PHONE or EMAIL
     * @param name Optional display name
     * @return Response containing userId, isVerified, isNew
     */
    public ResolveOrCreateResponse resolveOrCreateUser(String identityKey, String identityType, String name) {
        try {
            Map<String, Object> body = new java.util.HashMap<>();
            body.put("identityKey", identityKey);
            body.put("identityType", identityType);
            if (name != null) {
                body.put("name", name);
            }

            Map<String, Object> response = restClient.post()
                    .uri("/internal/users/resolve-or-create")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(new ParameterizedTypeReference<Map<String, Object>>() {});

            if (response == null || !Boolean.TRUE.equals(response.get("success"))) {
                logger.error("Auth-service returned unsuccessful response: {}", response);
                throw new RuntimeException("Auth-service returned unsuccessful response");
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> data = (Map<String, Object>) response.get("data");
            if (data == null) {
                throw new RuntimeException("Auth-service response missing data field");
            }

            return new ResolveOrCreateResponse(
                    (String) data.get("userId"),
                    Boolean.TRUE.equals(data.get("isVerified")),
                    Boolean.TRUE.equals(data.get("isNew")),
                    (String) data.get("phone"),
                    (String) data.get("email"),
                    (String) data.get("name")
            );

        } catch (Exception e) {
            logger.error("Failed to resolve/create user in auth-service: identityKey={}, error={}",
                    maskIdentifier(identityKey), e.getMessage());
            throw new RuntimeException("Failed to resolve/create user in auth-service: " + e.getMessage(), e);
        }
    }

    private String maskIdentifier(String identifier) {
        if (identifier == null || identifier.length() < 4) return "***";
        return identifier.substring(0, 3) + "***";
    }

    /**
     * Response from auth-service resolve-or-create endpoint.
     */
    public record ResolveOrCreateResponse(
            String userId,
            boolean isVerified,
            boolean isNew,
            String phone,
            String email,
            String name
    ) {}
}
