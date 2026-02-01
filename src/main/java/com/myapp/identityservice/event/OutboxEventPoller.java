package com.myapp.identityservice.event;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.myapp.identityservice.client.WowServiceClient;
import com.myapp.identityservice.domain.OutboundEvent;
import com.myapp.identityservice.domain.OutboundEvent.EventStatus;
import com.myapp.identityservice.repository.OutboundEventRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Polls outbound_events table and delivers events to downstream services via HTTP.
 * Handles retries with exponential backoff, tracks permanently failed events,
 * and includes a simple circuit breaker to prevent retry exhaustion during
 * downstream outages.
 *
 * <h3>Circuit Breaker</h3>
 * <ul>
 *   <li>CLOSED (normal): delivers events. Tracks consecutive delivery failures.</li>
 *   <li>OPEN (downstream unhealthy): skips polling. Entered after
 *       {@code failureThreshold} consecutive failures. Remains open for
 *       {@code cooldownSeconds} seconds.</li>
 *   <li>After cooldown expires, the next poll cycle attempts delivery again.
 *       A single success resets the failure counter (CLOSED). A failure
 *       re-opens the circuit.</li>
 * </ul>
 * This prevents burning through max_retries on all queued events during
 * a prolonged downstream outage, while still allowing genuine poison events
 * to reach PERMANENTLY_FAILED through normal retry exhaustion.
 */
@Component
@ConditionalOnProperty(name = "app.events.transport", havingValue = "http", matchIfMissing = true)
public class OutboxEventPoller {

    private static final Logger logger = LoggerFactory.getLogger(OutboxEventPoller.class);
    private static final TypeReference<Map<String, String>> MAP_TYPE = new TypeReference<>() {};

    private final OutboundEventRepository outboundEventRepository;
    private final WowServiceClient wowServiceClient;
    private final ObjectMapper objectMapper;
    private final Counter permanentlyFailedCounter;
    private final Counter circuitOpenCounter;

    @Value("${app.outbox.batch-size:50}")
    private int batchSize;

    @Value("${app.outbox.circuit-breaker.failure-threshold:5}")
    private int failureThreshold;

    @Value("${app.outbox.circuit-breaker.cooldown-seconds:60}")
    private long cooldownSeconds;

    // Circuit breaker state — accessed only from the single @Scheduled thread
    private int consecutiveFailures = 0;
    private Instant circuitOpenUntil = Instant.EPOCH;

    public OutboxEventPoller(OutboundEventRepository outboundEventRepository,
                              WowServiceClient wowServiceClient,
                              ObjectMapper objectMapper,
                              MeterRegistry meterRegistry) {
        this.outboundEventRepository = outboundEventRepository;
        this.wowServiceClient = wowServiceClient;
        this.objectMapper = objectMapper;
        this.permanentlyFailedCounter = Counter.builder("outbox.events.permanently_failed")
                .description("Count of events that exhausted all retries")
                .register(meterRegistry);
        this.circuitOpenCounter = Counter.builder("outbox.circuit_breaker.opened")
                .description("Count of times the circuit breaker opened")
                .register(meterRegistry);
    }

    @Scheduled(fixedDelayString = "${app.outbox.poll-interval-ms:5000}")
    @Transactional
    public void pollAndDeliver() {
        // Circuit breaker: skip if circuit is open
        if (isCircuitOpen()) {
            logger.debug("Outbox poller: circuit breaker OPEN, skipping until {}",
                    circuitOpenUntil);
            return;
        }

        List<OutboundEvent> events = outboundEventRepository.findEventsToDeliver(
                Instant.now(), PageRequest.of(0, batchSize));

        if (events.isEmpty()) {
            return;
        }

        logger.debug("Outbox poller: found {} events to deliver", events.size());

        for (OutboundEvent event : events) {
            // Re-check circuit after each event — stop immediately if it trips mid-batch
            if (isCircuitOpen()) {
                logger.info("Outbox poller: circuit breaker tripped mid-batch, stopping delivery");
                break;
            }

            try {
                deliverEvent(event);
                event.markDelivered();
                onDeliverySuccess();
                logger.info("Outbox: delivered event type={}, eventId={}",
                        event.getEventType(), event.getEventId());
            } catch (Exception e) {
                EventStatus previousStatus = event.getStatus();
                event.markFailed(e.getMessage());
                onDeliveryFailure();

                if (event.getStatus() == EventStatus.PERMANENTLY_FAILED
                        && previousStatus != EventStatus.PERMANENTLY_FAILED) {
                    permanentlyFailedCounter.increment();
                    logger.error("Outbox: event PERMANENTLY_FAILED — eventId={}, eventType={}, " +
                                    "retryCount={}, lastError={}, payload={}, createdAt={}",
                            event.getEventId(), event.getEventType(),
                            event.getRetryCount(), event.getLastError(),
                            event.getPayload(), event.getCreatedAt());
                } else {
                    logger.warn("Outbox: delivery failed, will retry — eventId={}, retryCount={}, " +
                                    "nextRetryAt={}, error={}",
                            event.getEventId(), event.getRetryCount(),
                            event.getNextRetryAt(), e.getMessage());
                }
            }
            outboundEventRepository.save(event);
        }
    }

    private void deliverEvent(OutboundEvent event) throws Exception {
        Map<String, String> payload = objectMapper.readValue(event.getPayload(), MAP_TYPE);

        switch (event.getEventType()) {
            case "INVITATION_ACCEPTED" -> wowServiceClient.notifyInvitationAccepted(
                    payload.get("invitationId"),
                    payload.get("targetUserId"),
                    payload.get("contextType"),
                    payload.get("contextId"),
                    payload.get("contextRole"),
                    payload.get("displayName"),
                    payload.get("addedBy"),
                    event.getEventId()
            );
            case "USER_NAME_UPDATED" -> wowServiceClient.notifyUserNameUpdated(
                    payload.get("userId"),
                    payload.get("newDisplayName"),
                    event.getEventId()
            );
            case "PENDING_USER_ACTION" -> {
                // Notification event — no downstream consumer yet.
                // Marked as delivered immediately. When a notification-service is
                // introduced, add a NotificationServiceClient call here.
                logger.info("Outbox: PENDING_USER_ACTION event for invitation={}, user={} — " +
                                "no notification consumer configured, marking delivered",
                        payload.get("invitationId"), payload.get("targetUserId"));
            }
            default -> throw new IllegalArgumentException("Unknown event type: " + event.getEventType());
        }
    }

    // --- Circuit breaker logic ---

    private boolean isCircuitOpen() {
        return consecutiveFailures >= failureThreshold
                && Instant.now().isBefore(circuitOpenUntil);
    }

    private void onDeliverySuccess() {
        if (consecutiveFailures > 0) {
            logger.info("Outbox poller: delivery succeeded, resetting circuit breaker " +
                    "(was at {} consecutive failures)", consecutiveFailures);
        }
        consecutiveFailures = 0;
    }

    private void onDeliveryFailure() {
        consecutiveFailures++;
        if (consecutiveFailures >= failureThreshold) {
            circuitOpenUntil = Instant.now().plusSeconds(cooldownSeconds);
            circuitOpenCounter.increment();
            logger.warn("Outbox poller: circuit breaker OPEN — {} consecutive failures, " +
                            "pausing delivery until {}",
                    consecutiveFailures, circuitOpenUntil);
        }
    }
}
