-- Migration V2: Add missing fields and indexes
-- Database: PostgreSQL

-- ===============
-- Add lastRetryAt to kafka_outbox
ALTER TABLE kafka_outbox
ADD COLUMN IF NOT EXISTS last_retry_at TIMESTAMP NULL;

-- ===============
-- Add version to saga_step (to match updated entity)
ALTER TABLE saga_step
ADD COLUMN IF NOT EXISTS version BIGINT NULL;

-- ===============
-- Add version to saga (if not exists)
ALTER TABLE saga
ADD COLUMN IF NOT EXISTS version BIGINT NULL;

-- ===============
-- Add index for kafka_outbox last_retry_at (for retry delay queries)
CREATE INDEX IF NOT EXISTS idx_kafka_outbox_last_retry_at ON kafka_outbox (last_retry_at);
