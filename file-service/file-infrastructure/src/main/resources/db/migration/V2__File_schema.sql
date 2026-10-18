-- File metadata.
--
-- The bytes live in object storage and never touch this database. What is here
-- is the record that decides whether an upload was allowed, and — after a
-- deleted — remembers which object still has to be removed.

CREATE TABLE stored_file (
    id UUID PRIMARY KEY,
    -- Unique: a key identifies exactly one object, and two records pointing at
    -- the same one would make deletion ambiguous.
    object_key VARCHAR(512) NOT NULL UNIQUE,
    original_name VARCHAR(512) NOT NULL,
    content_type VARCHAR(255) NOT NULL,
    size_bytes BIGINT NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    owner_id UUID NOT NULL,
    scope VARCHAR(64) NOT NULL,
    -- Null until the file is attached: a client asks for an upload ticket before
    -- the aggregate that will own the file necessarily exists.
    scope_id UUID,
    created_at TIMESTAMP NOT NULL,
    confirmed_at TIMESTAMP,
    version BIGINT,
    CONSTRAINT chk_stored_file_size CHECK (size_bytes > 0)
);

CREATE INDEX idx_stored_file_owner ON stored_file (owner_id);
CREATE INDEX idx_stored_file_scope ON stored_file (scope_id);

-- Both sweeps scan by status: abandoned uploads and objects awaiting removal.
CREATE INDEX idx_stored_file_status ON stored_file (status, created_at);
