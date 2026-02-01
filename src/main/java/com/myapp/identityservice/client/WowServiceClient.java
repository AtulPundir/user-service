package com.myapp.identityservice.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.HashMap;
import java.util.Map;

/**
 * Synchronous HTTP client for delivering events to wow-service.
 * Called by OutboxEventPoller — exceptions propagate to trigger retry logic.
 */
@Component
public class WowServiceClient {

    private static final Logger logger = LoggerFactory.getLogger(WowServiceClient.class);

    private final RestClient restClient;

    public WowServiceClient(@Value("${app.wow-service.base-url}") String baseUrl,
                              @Value("${app.wow-service.api-key}") String apiKey) {
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("X-API-Key", apiKey)
                .build();
    }

    public void notifyInvitationAccepted(String invitationId, String targetUserId,
                                          String contextType, String contextId,
                                          String contextRole, String displayName,
                                          String addedBy, String eventId) {
        Map<String, String> body = new HashMap<>();
        body.put("invitationId", invitationId);
        body.put("targetUserId", targetUserId);
        body.put("contextType", contextType);
        body.put("contextId", contextId);
        body.put("contextRole", contextRole != null ? contextRole : "MEMBER");
        body.put("displayName", displayName != null ? displayName : "");
        body.put("addedBy", addedBy != null ? addedBy : "");
        if (eventId != null) {
            body.put("eventId", eventId);
        }

        restClient.post()
                .uri("/internal/events/invitation-accepted")
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .toBodilessEntity();

        logger.info("Delivered to wow-service: INVITATION_ACCEPTED for contextType={}, contextId={}, userId={}",
                contextType, contextId, targetUserId);
    }

    public void notifyUserNameUpdated(String userId, String newDisplayName, String eventId) {
        Map<String, String> body = new HashMap<>();
        body.put("userId", userId);
        body.put("newDisplayName", newDisplayName);
        if (eventId != null) {
            body.put("eventId", eventId);
        }

        restClient.post()
                .uri("/internal/events/user-name-updated")
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .toBodilessEntity();

        logger.info("Delivered to wow-service: USER_NAME_UPDATED for userId={}", userId);
    }
}
