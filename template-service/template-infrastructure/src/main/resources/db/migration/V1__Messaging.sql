-- Transactional outbox, idempotent-receiver ledger and saga log.
--
-- Written by the shared messaging and saga modules, not by this service's domain: the shape is
-- theirs and a clone copies it verbatim. See docs/eventual-consistency.md.
--
-- kafka_outbox is written in the same transaction as the business change that produced the
-- event, which is what makes publication atomic with the state it announces. processed_event is
-- claimed in the consuming handler's transaction, so a failed handler leaves nothing claimed and
-- the redelivery is a real retry.

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

CREATE TABLE saga (
    id VARCHAR(255) PRIMARY KEY,
    type VARCHAR(255) NOT NULL,
    status VARCHAR(50) NOT NULL,
    payload TEXT NOT NULL,
    last_error TEXT NULL,
    started_at TIMESTAMP NOT NULL,
    completed_at TIMESTAMP NULL,
    updated_at TIMESTAMP NOT NULL,
    version BIGINT NULL
);
CREATE INDEX idx_saga_status ON saga (status);
CREATE INDEX idx_saga_type ON saga (type);
CREATE INDEX idx_saga_started_at ON saga (started_at);

CREATE TABLE saga_step (
    id BIGSERIAL PRIMARY KEY,
    saga_id VARCHAR(255) NOT NULL,
    step_name VARCHAR(255) NOT NULL,
    status VARCHAR(50) NOT NULL,
    payload TEXT NULL,
    error_message TEXT NULL,
    created_at TIMESTAMP NOT NULL,
    completed_at TIMESTAMP NULL,
    compensation_step_id BIGINT NULL,
    version BIGINT NULL,

    CONSTRAINT uk_saga_step UNIQUE (saga_id, step_name),
    CONSTRAINT fk_saga_step_saga FOREIGN KEY (saga_id) REFERENCES saga(id) ON DELETE CASCADE
);
CREATE INDEX idx_saga_step_status ON saga_step (status);
