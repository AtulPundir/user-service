-- Transactional Outbox table for durable event delivery
-- Events are written in the same transaction as business state changes,
-- then delivered asynchronously by the OutboxEventPoller.

CREATE TABLE outbound_events (
    id              VARCHAR(30)  PRIMARY KEY,
    event_type      VARCHAR(30)  NOT NULL,
    event_id        VARCHAR(50)  NOT NULL UNIQUE,
    payload         JSONB        NOT NULL,
    status          VARCHAR(25)  NOT NULL DEFAULT 'PENDING',
    retry_count     INT          NOT NULL DEFAULT 0,
    max_retries     INT          NOT NULL DEFAULT 5,
    next_retry_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    last_error      TEXT,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

-- Partial index for efficient polling: only scan PENDING/FAILED events
CREATE INDEX idx_outbound_events_poll
    ON outbound_events (next_retry_at)
    WHERE status IN ('PENDING', 'FAILED');

-- Index for admin inspection of permanently failed events
CREATE INDEX idx_outbound_events_failed
    ON outbound_events (created_at)
    WHERE status = 'PERMANENTLY_FAILED';
