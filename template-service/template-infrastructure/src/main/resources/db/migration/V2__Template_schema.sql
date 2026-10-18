-- The one domain table of the reference service.
--
-- Deliberately trivial: a clone keeps the shape — a string id assigned by the domain, an enum
-- persisted by name, and the two timestamps every aggregate carries — and replaces the columns
-- that carry meaning.
--
-- The id is a VARCHAR rather than a generated key because an aggregate needs its identity before
-- the transaction commits, so the outbox event it produces can reference it.

CREATE TABLE template (
    id VARCHAR(255) PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    payload TEXT NOT NULL,
    status VARCHAR(50) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);
CREATE INDEX idx_template_status ON template (status);
