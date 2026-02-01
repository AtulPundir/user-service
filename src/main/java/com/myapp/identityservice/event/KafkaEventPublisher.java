package com.myapp.identityservice.event;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@ConditionalOnProperty(name = "app.events.transport", havingValue = "kafka")
public class KafkaEventPublisher implements EventPublisher {

    private static final Logger logger = LoggerFactory.getLogger(KafkaEventPublisher.class);

    public static final String TOPIC_INVITATION_ACCEPTED = "identity.invitation-accepted";
    public static final String TOPIC_USER_NAME_UPDATED = "identity.user-name-updated";
    public static final String TOPIC_PENDING_USER_ACTION = "identity.pending-user-action";

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public KafkaEventPublisher(KafkaTemplate<String, String> kafkaTemplate,
                                ObjectMapper objectMapper) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public void publishInvitationAccepted(String invitationId, String targetUserId,
                                            String contextType, String contextId,
                                            String contextRole, String displayName,
                                            String addedBy) {
        Map<String, String> payload = Map.of(
                "invitationId", invitationId,
                "targetUserId", targetUserId,
                "contextType", contextType,
                "contextId", contextId,
                "contextRole", contextRole != null ? contextRole : "MEMBER",
                "displayName", displayName != null ? displayName : "",
                "addedBy", addedBy != null ? addedBy : ""
        );

        try {
            String json = objectMapper.writeValueAsString(payload);
            // Use contextId as key for partition locality (events for same context go to same partition)
            kafkaTemplate.send(TOPIC_INVITATION_ACCEPTED, contextId, json)
                    .whenComplete((result, ex) -> {
                        if (ex != null) {
                            logger.error("Failed to publish INVITATION_ACCEPTED to Kafka: invitationId={}, error={}",
                                    invitationId, ex.getMessage());
                        } else {
                            logger.info("Published INVITATION_ACCEPTED to Kafka: invitationId={}, contextType={}, contextId={}",
                                    invitationId, contextType, contextId);
                        }
                    });
        } catch (JsonProcessingException e) {
            logger.error("Failed to serialize INVITATION_ACCEPTED event: {}", e.getMessage());
        }
    }

    @Override
    public void publishUserNameUpdated(String userId, String newDisplayName, long updatedAtMillis) {
        Map<String, String> payload = Map.of(
                "userId", userId,
                "newDisplayName", newDisplayName
        );

        try {
            String json = objectMapper.writeValueAsString(payload);
            kafkaTemplate.send(TOPIC_USER_NAME_UPDATED, userId, json)
                    .whenComplete((result, ex) -> {
                        if (ex != null) {
                            logger.error("Failed to publish USER_NAME_UPDATED to Kafka: userId={}, error={}",
                                    userId, ex.getMessage());
                        } else {
                            logger.info("Published USER_NAME_UPDATED to Kafka: userId={}", userId);
                        }
                    });
        } catch (JsonProcessingException e) {
            logger.error("Failed to serialize USER_NAME_UPDATED event: {}", e.getMessage());
        }
    }

    @Override
    public void publishPendingUserAction(String invitationId, String targetUserId,
                                           String contextType, String contextId) {
        Map<String, String> payload = Map.of(
                "invitationId", invitationId,
                "targetUserId", targetUserId,
                "contextType", contextType,
                "contextId", contextId
        );

        try {
            String json = objectMapper.writeValueAsString(payload);
            kafkaTemplate.send(TOPIC_PENDING_USER_ACTION, targetUserId, json)
                    .whenComplete((result, ex) -> {
                        if (ex != null) {
                            logger.error("Failed to publish PENDING_USER_ACTION to Kafka: invitationId={}, error={}",
                                    invitationId, ex.getMessage());
                        } else {
                            logger.info("Published PENDING_USER_ACTION to Kafka: invitationId={}", invitationId);
                        }
                    });
        } catch (JsonProcessingException e) {
            logger.error("Failed to serialize PENDING_USER_ACTION event: {}", e.getMessage());
        }
    }
}
