-- Transactional outbox and idempotent-receiver ledger.
--
-- Written by the shared messaging module, not by this service's domain: the shape is theirs
-- and a clone copies it verbatim. See docs/eventual-consistency.md.
--
-- kafka_outbox is written in the same transaction as the business change that produced the
-- event, which is what makes publication atomic with the state it announces. processed_event is
-- claimed in the consuming handler's transaction, so a failed handler leaves nothing claimed and
-- the redelivery is a real retry.
--
-- saga_id carries a correlation value copied from an inbound event. This service runs no saga
-- of its own, so there is no saga log here.

CREATE TABLE kafka_outbox (
    id BIGSERIAL PRIMARY KEY,
    event_id VARCHAR(255) NOT NULL,
    topic VARCHAR(255) NOT NULL,
    key VARCHAR(255) NOT NULL,
    payload BYTEA NOT NULL,
    status VARCHAR(50) NOT NULL,
    error_message TEXT NULL,
    created_at TIMESTAMP NOT NULL,
    processed_at TIMESTAMP NULL,
    retry_count INT NOT NULL DEFAULT 0,
    last_retry_at TIMESTAMP NULL,
    saga_id VARCHAR(255) NULL,
    version BIGINT NULL,

    CONSTRAINT uk_kafka_outbox_event_id UNIQUE (event_id)
);
CREATE INDEX idx_kafka_outbox_created_at ON kafka_outbox (created_at);
CREATE INDEX idx_kafka_outbox_topic ON kafka_outbox (topic);
CREATE INDEX idx_kafka_outbox_last_retry_at ON kafka_outbox (last_retry_at);
CREATE INDEX idx_kafka_outbox_dispatch ON kafka_outbox (status, retry_count, last_retry_at);
CREATE INDEX idx_kafka_outbox_processed_at ON kafka_outbox (processed_at);

CREATE TABLE processed_event (
    id BIGSERIAL PRIMARY KEY,
    event_id VARCHAR(255) NOT NULL,
    consumer_group VARCHAR(255) NOT NULL,
    processed_at TIMESTAMP NOT NULL,

    CONSTRAINT uk_processed_event_event_id_consumer UNIQUE (event_id, consumer_group)
);
CREATE INDEX idx_processed_event_processed_at ON processed_event (processed_at);
