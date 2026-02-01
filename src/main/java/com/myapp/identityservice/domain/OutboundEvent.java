package com.myapp.identityservice.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "outbound_events")
public class OutboundEvent {

    public enum EventStatus {
        PENDING, DELIVERED, FAILED, PERMANENTLY_FAILED
    }

    @Id
    @Column(length = 30)
    private String id;

    @Column(name = "event_type", length = 30, nullable = false)
    private String eventType;

    @Column(name = "event_id", length = 50, nullable = false, unique = true)
    private String eventId;

    @Column(columnDefinition = "jsonb", nullable = false)
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(length = 25, nullable = false)
    private EventStatus status = EventStatus.PENDING;

    @Column(name = "retry_count", nullable = false)
    private int retryCount = 0;

    @Column(name = "max_retries", nullable = false)
    private int maxRetries = 5;

    @Column(name = "next_retry_at", nullable = false)
    private Instant nextRetryAt;

    @Column(name = "last_error", columnDefinition = "TEXT")
    private String lastError;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
        if (this.nextRetryAt == null) {
            this.nextRetryAt = now;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }

    // Business methods

    public void markDelivered() {
        this.status = EventStatus.DELIVERED;
    }

    public void markFailed(String error) {
        this.retryCount++;
        this.lastError = error;
        if (this.retryCount >= this.maxRetries) {
            this.status = EventStatus.PERMANENTLY_FAILED;
        } else {
            this.status = EventStatus.FAILED;
            // Exponential backoff: 30s * 2^retryCount, capped at 1 hour
            long delaySeconds = Math.min(30L * (1L << this.retryCount), 3600L);
            this.nextRetryAt = Instant.now().plusSeconds(delaySeconds);
        }
    }

    public void resetForRetry() {
        this.status = EventStatus.PENDING;
        this.retryCount = 0;
        this.nextRetryAt = Instant.now();
        this.lastError = null;
    }

    // Getters and Setters

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }

    public String getEventId() { return eventId; }
    public void setEventId(String eventId) { this.eventId = eventId; }

    public String getPayload() { return payload; }
    public void setPayload(String payload) { this.payload = payload; }

    public EventStatus getStatus() { return status; }
    public void setStatus(EventStatus status) { this.status = status; }

    public int getRetryCount() { return retryCount; }
    public int getMaxRetries() { return maxRetries; }
    public void setMaxRetries(int maxRetries) { this.maxRetries = maxRetries; }

    public Instant getNextRetryAt() { return nextRetryAt; }
    public void setNextRetryAt(Instant nextRetryAt) { this.nextRetryAt = nextRetryAt; }

    public String getLastError() { return lastError; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
