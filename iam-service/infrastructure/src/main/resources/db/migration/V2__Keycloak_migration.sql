-- Migration to Keycloak: remove password-based auth, add keycloak_id and permissions

-- Add keycloak_id to user table
ALTER TABLE "user" ADD COLUMN keycloak_id UUID NULL;
CREATE UNIQUE INDEX IF NOT EXISTS idx_user_keycloak_id ON "user" (keycloak_id);

-- Drop password and enabled columns (Keycloak manages these)
ALTER TABLE "user" DROP COLUMN IF EXISTS password;
ALTER TABLE "user" DROP COLUMN IF EXISTS enabled;
DROP INDEX IF EXISTS idx_user_enabled;

-- Drop refresh_token table (Keycloak manages refresh tokens)
DROP TABLE IF EXISTS refresh_token;

-- Create permission table
CREATE TABLE IF NOT EXISTS permission (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    version BIGINT NULL,

    CONSTRAINT uk_permission_name UNIQUE (name)
);
CREATE INDEX IF NOT EXISTS idx_permission_name ON permission (name);

-- Create user_permission_mapping table
CREATE TABLE IF NOT EXISTS user_permission_mapping (
    user_id BIGINT NOT NULL,
    permission_id BIGINT NOT NULL,
    PRIMARY KEY (user_id, permission_id),
    CONSTRAINT fk_user_permission_mapping_user FOREIGN KEY (user_id) REFERENCES "user"(id) ON DELETE CASCADE,
    CONSTRAINT fk_user_permission_mapping_permission FOREIGN KEY (permission_id) REFERENCES permission(id) ON DELETE CASCADE
);
