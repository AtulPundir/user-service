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
 * Client for calling notification-service internal endpoints.
 * Handles all notification delivery for identity-service events.
 */
@Component
public class NotificationServiceClient {

    private static final Logger logger = LoggerFactory.getLogger(NotificationServiceClient.class);
    private static final String API_KEY_HEADER = "X-API-Key";

    private final RestClient restClient;
    private final String apiKey;

    public NotificationServiceClient(
            @Value("${app.notification-service.base-url:http://localhost:8083}") String baseUrl,
            @Value("${app.notification-service.api-key:internal-service-key}") String apiKey) {
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .build();
        this.apiKey = apiKey;
    }

    // ==================== Invitation Notifications ====================

    /**
     * Notify invitee about a new invitation (Trip, Collaboration, ExpenseGroup, Agreement).
     */
    public void sendInvitationCreated(String recipientPhone, String recipientEmail,
                                       String inviterName, String contextType, String contextName,
                                       String message) {
        Map<String, Object> data = new HashMap<>();
        data.put("inviterName", inviterName);
        data.put("contextType", formatContextType(contextType));
        data.put("contextName", contextName != null ? contextName : "");
        data.put("message", message != null ? message : "");

        // Send SMS if phone available
        if (recipientPhone != null && !recipientPhone.isBlank()) {
            sendNotification("INVITATION_CREATED_SMS", "SMS", recipientPhone, data,
                    "INV_CREATED_SMS:" + recipientPhone + ":" + System.currentTimeMillis());
        }

        // Send Email if email available
        if (recipientEmail != null && !recipientEmail.isBlank()) {
            sendNotification("INVITATION_CREATED_EMAIL", "EMAIL", recipientEmail, data,
                    "INV_CREATED_EMAIL:" + recipientEmail + ":" + System.currentTimeMillis());
        }
    }

    /**
     * Notify inviter that their invitation was accepted.
     */
    public void sendInvitationAccepted(String inviterPhone, String inviterEmail,
                                        String inviteeName, String contextType, String contextName) {
        Map<String, Object> data = new HashMap<>();
        data.put("inviteeName", inviteeName);
        data.put("contextType", formatContextType(contextType));
        data.put("contextName", contextName != null ? contextName : "");

        // Send SMS to inviter
        if (inviterPhone != null && !inviterPhone.isBlank()) {
            sendNotification("INVITATION_ACCEPTED_SMS", "SMS", inviterPhone, data,
                    "INV_ACCEPTED_SMS:" + inviterPhone + ":" + System.currentTimeMillis());
        }
    }

    /**
     * Notify users who invited a placeholder that the user is now verified.
     */
    public void sendUserVerified(String recipientPhone, String recipientEmail,
                                  String verifiedUserName) {
        Map<String, Object> data = new HashMap<>();
        data.put("userName", verifiedUserName);

        // Send SMS notification
        if (recipientPhone != null && !recipientPhone.isBlank()) {
            sendNotification("USER_VERIFIED_SMS", "SMS", recipientPhone, data,
                    "USR_VERIFIED_SMS:" + recipientPhone + ":" + System.currentTimeMillis());
        }
    }

    // ==================== Helper Methods ====================

    private void sendNotification(String templateKey, String channel, String recipient,
                                   Map<String, Object> data, String idempotencyKey) {
        try {
            Map<String, Object> request = new HashMap<>();
            request.put("templateKey", templateKey);
            request.put("channel", channel);
            request.put("recipient", recipient);
            request.put("data", data);
            request.put("priority", "NORMAL");
            request.put("idempotencyKey", idempotencyKey);

            restClient.post()
                    .uri("/internal/notifications/send")
                    .header(API_KEY_HEADER, apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .toBodilessEntity();

            logger.info("Notification sent: template={}, channel={}, recipient={}",
                    templateKey, channel, maskRecipient(recipient));
        } catch (Exception e) {
            // Log but don't fail - notifications are best-effort
            logger.error("Failed to send notification: template={}, channel={}, recipient={}, error={}",
                    templateKey, channel, maskRecipient(recipient), e.getMessage());
        }
    }

    private String formatContextType(String contextType) {
        if (contextType == null) return "";
        return switch (contextType.toUpperCase()) {
            case "TRIP" -> "trip";
            case "COLLABORATION" -> "collaboration";
            case "EXPENSE_GROUP" -> "expense group";
            case "AGREEMENT" -> "agreement";
            default -> contextType.toLowerCase();
        };
    }

    private String maskRecipient(String recipient) {
        if (recipient == null || recipient.length() < 4) return "****";
        if (recipient.contains("@")) {
            int atIndex = recipient.indexOf("@");
            return recipient.substring(0, Math.min(3, atIndex)) + "***" + recipient.substring(atIndex);
        }
        if (recipient.length() > 6) {
            return recipient.substring(0, 4) + "****" + recipient.substring(recipient.length() - 2);
        }
        return "****";
    }
}
