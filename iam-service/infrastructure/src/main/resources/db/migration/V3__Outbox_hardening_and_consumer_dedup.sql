-- Migration V3: outbox hardening + consumer-side dedup
-- Database: PostgreSQL

-- ===============
-- kafka_outbox: enforce unique event_id (required by transactional outbox);
-- duplicate inserts must fail so producers can detect business-logic retries.
ALTER TABLE kafka_outbox
ADD CONSTRAINT uk_kafka_outbox_event_id UNIQUE (event_id);

-- Composite index that backs `lockBatchForDispatch` (status + retry_count + last_retry_at).
CREATE INDEX IF NOT EXISTS idx_kafka_outbox_dispatch
    ON kafka_outbox (status, retry_count, last_retry_at);

-- Index that backs the stuck-PROCESSING reaper branch of `lockBatchForDispatch`.
CREATE INDEX IF NOT EXISTS idx_kafka_outbox_processed_at
    ON kafka_outbox (processed_at);

-- ===============
-- processed_event: consumer-side idempotent receiver pattern.
-- Each (event_id, consumer_group) pair may exist at most once; the unique
-- violation is the signal that a duplicate message arrived.
CREATE TABLE IF NOT EXISTS processed_event (
    id BIGSERIAL PRIMARY KEY,
    event_id VARCHAR(255) NOT NULL,
    consumer_group VARCHAR(255) NOT NULL,
    processed_at TIMESTAMP NOT NULL,
    CONSTRAINT uk_processed_event_event_id_consumer UNIQUE (event_id, consumer_group)
);
CREATE INDEX IF NOT EXISTS idx_processed_event_processed_at ON processed_event (processed_at);

