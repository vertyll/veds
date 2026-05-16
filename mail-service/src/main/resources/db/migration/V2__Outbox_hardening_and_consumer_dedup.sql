-- Migration V2: outbox hardening + consumer-side dedup
-- Database: PostgreSQL

ALTER TABLE kafka_outbox
ADD CONSTRAINT uk_kafka_outbox_event_id UNIQUE (event_id);

CREATE INDEX IF NOT EXISTS idx_kafka_outbox_dispatch
    ON kafka_outbox (status, retry_count, last_retry_at);

CREATE INDEX IF NOT EXISTS idx_kafka_outbox_processed_at
    ON kafka_outbox (processed_at);

CREATE TABLE IF NOT EXISTS processed_event (
    id BIGSERIAL PRIMARY KEY,
    event_id VARCHAR(255) NOT NULL,
    consumer_group VARCHAR(255) NOT NULL,
    processed_at TIMESTAMP NOT NULL,
    CONSTRAINT uk_processed_event_event_id_consumer UNIQUE (event_id, consumer_group)
);
CREATE INDEX IF NOT EXISTS idx_processed_event_processed_at ON processed_event (processed_at);

