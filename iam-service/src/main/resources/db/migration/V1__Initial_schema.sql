-- Initial schema for identity-service
-- Database: PostgreSQL

-- ===============
-- kafka_outbox
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
-- user
CREATE TABLE IF NOT EXISTS "user" (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) NOT NULL,
    password VARCHAR(255) NOT NULL,
    first_name VARCHAR(255) NOT NULL,
    last_name VARCHAR(255) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT FALSE,
    profile_picture VARCHAR(255) NULL,
    phone_number VARCHAR(255) NULL,
    address VARCHAR(500) NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    version BIGINT NULL,

    CONSTRAINT uk_user_email UNIQUE (email)
);
CREATE INDEX IF NOT EXISTS idx_user_email ON "user" (email);
CREATE INDEX IF NOT EXISTS idx_user_enabled ON "user" (enabled);
CREATE INDEX IF NOT EXISTS idx_user_created_at ON "user" (created_at);

-- ===============
-- role
CREATE TABLE IF NOT EXISTS role (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    version BIGINT NULL,

    CONSTRAINT uk_role_name UNIQUE (name)
);
CREATE INDEX IF NOT EXISTS idx_role_name ON role (name);

-- ===============
-- user_role_mapping
CREATE TABLE IF NOT EXISTS user_role_mapping (
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    PRIMARY KEY (user_id, role_id),
    CONSTRAINT fk_user_role_mapping_user FOREIGN KEY (user_id) REFERENCES "user"(id) ON DELETE CASCADE,
    CONSTRAINT fk_user_role_mapping_role FOREIGN KEY (role_id) REFERENCES role(id) ON DELETE CASCADE
);

-- ===============
-- refresh_token
CREATE TABLE IF NOT EXISTS refresh_token (
    id BIGSERIAL PRIMARY KEY,
    token VARCHAR(1024) NOT NULL,
    username VARCHAR(255) NOT NULL,
    expiry_date TIMESTAMP NOT NULL,
    revoked BOOLEAN NOT NULL DEFAULT FALSE,
    device_info TEXT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    version BIGINT NULL,

    CONSTRAINT uk_refresh_token_token UNIQUE (token)
);
CREATE INDEX IF NOT EXISTS idx_refresh_token_token ON refresh_token (token);
CREATE INDEX IF NOT EXISTS idx_refresh_token_username ON refresh_token (username);

-- ===============
-- verification_token
CREATE TABLE IF NOT EXISTS verification_token (
    id BIGSERIAL PRIMARY KEY,
    token VARCHAR(1024) NOT NULL,
    username VARCHAR(255) NOT NULL,
    expiry_date TIMESTAMP NOT NULL,
    used BOOLEAN NOT NULL DEFAULT FALSE,
    token_type VARCHAR(255) NOT NULL,
    additional_data TEXT NULL,
    saga_id VARCHAR(255) NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    version BIGINT NULL,

    CONSTRAINT uk_verification_token_token UNIQUE (token)
);
CREATE INDEX IF NOT EXISTS idx_verification_token_token ON verification_token (token);
CREATE INDEX IF NOT EXISTS idx_verification_token_username ON verification_token (username);

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
