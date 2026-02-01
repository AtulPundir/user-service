package com.myapp.identityservice.event;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.myapp.identityservice.domain.OutboundEvent;
import com.myapp.identityservice.repository.OutboundEventRepository;
import com.myapp.identityservice.util.CuidGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Transactional Outbox publisher — writes events to outbound_events table
 * within the caller's transaction. Events are delivered asynchronously
 * by OutboxEventPoller.
 *
 * This replaces HttpEventPublisher for durable, retryable event delivery.
 */
@Component
@ConditionalOnProperty(name = "app.events.transport", havingValue = "http", matchIfMissing = true)
public class OutboxEventPublisher implements EventPublisher {

    private static final Logger logger = LoggerFactory.getLogger(OutboxEventPublisher.class);

    private final OutboundEventRepository outboundEventRepository;
    private final CuidGenerator cuidGenerator;
    private final ObjectMapper objectMapper;

    public OutboxEventPublisher(OutboundEventRepository outboundEventRepository,
                                 CuidGenerator cuidGenerator,
                                 ObjectMapper objectMapper) {
        this.outboundEventRepository = outboundEventRepository;
        this.cuidGenerator = cuidGenerator;
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

        // Deterministic eventId from invitationId — same invitation acceptance always produces same eventId
        String eventId = "INV_ACC:" + invitationId;
        saveOutboundEvent("INVITATION_ACCEPTED", eventId, payload);
    }

    @Override
    public void publishUserNameUpdated(String userId, String newDisplayName, long updatedAtMillis) {
        Map<String, String> payload = Map.of(
                "userId", userId,
                "newDisplayName", newDisplayName
        );

        // Deterministic eventId from User.updatedAt — retries of the same logical
        // name change produce the same eventId, enabling exactly-once dedup in consumers.
        // A subsequent name change yields a different updatedAt and therefore a new eventId.
        String eventId = "USR_UPD:" + userId + ":" + updatedAtMillis;
        saveOutboundEvent("USER_NAME_UPDATED", eventId, payload);
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

        // Deterministic — one notification per invitation transition
        String eventId = "PUA:" + invitationId;
        saveOutboundEvent("PENDING_USER_ACTION", eventId, payload);
    }

    private void saveOutboundEvent(String eventType, String eventId, Map<String, String> payload) {
        try {
            OutboundEvent event = new OutboundEvent();
            event.setId(cuidGenerator.generate());
            event.setEventType(eventType);
            event.setEventId(eventId);
            event.setPayload(objectMapper.writeValueAsString(payload));
            outboundEventRepository.save(event);

            logger.debug("Outbox: saved event type={}, eventId={}", eventType, eventId);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize outbound event payload", e);
        }
    }
}
