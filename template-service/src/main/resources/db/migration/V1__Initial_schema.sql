-- Flyway initial schema for template-service based on JPA entities
-- Database: PostgreSQL

-- ===============
-- kafka_outbox (shared)
CREATE TABLE IF NOT EXISTS kafka_outbox (
    id BIGSERIAL PRIMARY KEY,
    event_id VARCHAR(255) NOT NULL,
    topic VARCHAR(255) NOT NULL,
    key VARCHAR(255) NOT NULL,
    payload TEXT NOT NULL,
    status VARCHAR(50) NOT NULL,
    error_message TEXT NULL,
    created_at TIMESTAMP NOT NULL,
    processed_at TIMESTAMP NULL,
    retry_count INT NOT NULL DEFAULT 0,
    last_retry_at TIMESTAMP NULL,
    saga_id VARCHAR(255) NULL,
    version BIGINT NULL
);
CREATE INDEX IF NOT EXISTS idx_kafka_outbox_status ON kafka_outbox (status);
CREATE INDEX IF NOT EXISTS idx_kafka_outbox_created_at ON kafka_outbox (created_at);
CREATE INDEX IF NOT EXISTS idx_kafka_outbox_topic ON kafka_outbox (topic);
CREATE INDEX IF NOT EXISTS idx_kafka_outbox_last_retry_at ON kafka_outbox (last_retry_at);

-- ===============
-- saga
CREATE TABLE IF NOT EXISTS saga (
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
CREATE INDEX IF NOT EXISTS idx_saga_status ON saga (status);
CREATE INDEX IF NOT EXISTS idx_saga_type ON saga (type);
CREATE INDEX IF NOT EXISTS idx_saga_started_at ON saga (started_at);

-- ===============
-- saga_step
CREATE TABLE IF NOT EXISTS saga_step (
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
CREATE INDEX IF NOT EXISTS idx_saga_step_saga_id ON saga_step (saga_id);
CREATE INDEX IF NOT EXISTS idx_saga_step_status ON saga_step (status);
