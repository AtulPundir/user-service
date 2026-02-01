package com.myapp.identityservice.event;

import com.myapp.identityservice.repository.OutboundEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * Periodic cleanup of delivered outbound events.
 *
 * <h3>Retention Policy</h3>
 * <ul>
 *   <li>DELIVERED events: deleted after {@code retention-days} (default 30 days).
 *       These have been successfully processed by consumers and their eventIds
 *       exist in wow-service's processed_events table, so they are no longer
 *       needed for retry or deduplication.</li>
 *   <li>PERMANENTLY_FAILED events: retained indefinitely until manual resolution
 *       via the admin endpoint ({@code POST /internal/admin/outbox/{id}/retry}).</li>
 *   <li>PENDING / FAILED events: never cleaned — still awaiting delivery.</li>
 * </ul>
 *
 * <h3>Why 30 days is safe</h3>
 * Max retry window is ~16 minutes (5 retries with exponential backoff capped at 1h).
 * An event that reaches DELIVERED status will never be retried. The 30-day buffer
 * exists solely for operational forensics (log correlation, debugging).
 */
@Component
public class OutboxCleanupJob {

    private static final Logger logger = LoggerFactory.getLogger(OutboxCleanupJob.class);

    private final OutboundEventRepository outboundEventRepository;

    @Value("${app.outbox.cleanup.retention-days:30}")
    private int retentionDays;

    public OutboxCleanupJob(OutboundEventRepository outboundEventRepository) {
        this.outboundEventRepository = outboundEventRepository;
    }

    @Scheduled(cron = "${app.outbox.cleanup.cron:0 30 3 * * *}")
    @Transactional
    public void cleanupDeliveredEvents() {
        Instant cutoff = Instant.now().minus(retentionDays, ChronoUnit.DAYS);
        int deleted = outboundEventRepository.deleteDeliveredOlderThan(cutoff);

        if (deleted > 0) {
            logger.info("Outbox cleanup: deleted {} delivered events older than {} days",
                    deleted, retentionDays);
        } else {
            logger.debug("Outbox cleanup: no delivered events to purge");
        }
    }
}
