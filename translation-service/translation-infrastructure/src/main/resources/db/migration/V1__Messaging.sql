-- The idempotent-receiver ledger. This service consumes one event and publishes
-- none, so it carries the inbox and no outbox. Claimed in the handler's
-- transaction, so a failed handler leaves nothing claimed.
CREATE TABLE processed_event (
    id BIGSERIAL PRIMARY KEY,
    event_id VARCHAR(255) NOT NULL,
    consumer_group VARCHAR(255) NOT NULL,
    processed_at TIMESTAMP NOT NULL,

    CONSTRAINT uk_processed_event_event_id_consumer UNIQUE (event_id, consumer_group)
);
CREATE INDEX idx_processed_event_processed_at ON processed_event (processed_at);
