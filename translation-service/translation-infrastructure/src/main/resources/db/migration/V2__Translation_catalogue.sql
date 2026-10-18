-- Translation catalogue.
--
-- No outbox, no saga tables: this service publishes no integration events and
-- takes part in no distributed flow. It is a catalogue that other services read
-- and an administrator edits.

-- Languages the application offers. Seeded from code — adding one is a
-- deployment decision, not a runtime action, which is why there is no API to
-- create them.
CREATE TABLE language (
    tag VARCHAR(16) PRIMARY KEY,
    display_name VARCHAR(128) NOT NULL,
    is_default BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL
);

-- Exactly one default language: it is what an import template and a new key are
-- pre-filled from, so two would make that ambiguous.
CREATE UNIQUE INDEX uq_language_single_default
    ON language (is_default)
    WHERE is_default = TRUE;

-- Keys, identified by the key itself. The key is stable and every lookup is by
-- it, so a surrogate id would only add a join.
CREATE TABLE translation_key (
    translation_key VARCHAR(255) PRIMARY KEY,
    source_service VARCHAR(64) NOT NULL,
    description TEXT,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE INDEX idx_translation_key_source ON translation_key (source_service);

-- What one key says in one language.
--
-- `default_value` belongs to the seeding service, `override_value` to the
-- administrator. Two columns rather than one is the load-bearing decision here:
-- a single column written by both would mean every redeploy silently reverts
-- somebody's correction.
CREATE TABLE translation_value (
    id UUID PRIMARY KEY,
    translation_key VARCHAR(255) NOT NULL REFERENCES translation_key (translation_key) ON DELETE CASCADE,
    language VARCHAR(16) NOT NULL REFERENCES language (tag),
    default_value TEXT,
    override_value TEXT,
    updated_by UUID,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    version BIGINT,
    CONSTRAINT uq_translation_value_key_language UNIQUE (translation_key, language)
);

-- The public snapshot reads one language at a time.

-- Backs the snapshot's ETag: MAX(updated_at) per language.
CREATE INDEX idx_translation_value_updated ON translation_value (language, updated_at);

-- What each role grants, as announced by iam-service. Administration of the
-- catalogue is guarded by a permission, and the answer has to be local: asking
-- iam on every request would make translations unreadable whenever it is down.
CREATE TABLE role_permission_projection (
    role_name VARCHAR(64) PRIMARY KEY,
    unrestricted BOOLEAN NOT NULL DEFAULT FALSE,
    updated_at TIMESTAMP NOT NULL
);

CREATE TABLE role_permission_projection_permission (
    role_name VARCHAR(64) NOT NULL REFERENCES role_permission_projection (role_name) ON DELETE CASCADE,
    permission VARCHAR(128) NOT NULL,

    PRIMARY KEY (role_name, permission)
);
