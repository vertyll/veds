-- Notification bounded context schema.
--
-- No foreign keys to users, projects or tasks: those rows live in other
-- services' databases. `subject_id` and `project_id` are opaque references the
-- client uses to deep-link, and `recipient_ref` is a projection, not a copy of
-- the IAM user table.

CREATE TABLE notification (
    id UUID PRIMARY KEY,
    recipient_id UUID NOT NULL,
    type VARCHAR(64) NOT NULL,
    project_id UUID,
    subject_id UUID,
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    read_at TIMESTAMP,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL,
    version BIGINT
);

CREATE INDEX idx_notification_subject ON notification (subject_id);

-- The bell badge polls the unread count constantly; this is the index it uses.
CREATE INDEX idx_notification_unread
    ON notification (recipient_id, is_read, is_active);

-- Interpolation arguments for the client-side message. Relational rather than a
-- JSON column so a query can filter on one without a PostgreSQL-specific operator.
CREATE TABLE notification_param (
    notification_id UUID NOT NULL REFERENCES notification (id) ON DELETE CASCADE,
    param_key VARCHAR(64) NOT NULL,
    param_value VARCHAR(512) NOT NULL,
    CONSTRAINT uq_notification_param UNIQUE (notification_id, param_key)
);

-- ===============
-- Delivery preferences. A missing row means defaults, never "notify nothing".
CREATE TABLE notification_settings (
    user_id UUID PRIMARY KEY,
    version BIGINT
);

CREATE TABLE notification_muted_type (
    user_id UUID NOT NULL REFERENCES notification_settings (user_id) ON DELETE CASCADE,
    type VARCHAR(64) NOT NULL,
    CONSTRAINT uq_notification_muted_type UNIQUE (user_id, type)
);

CREATE TABLE notification_email_type (
    user_id UUID NOT NULL REFERENCES notification_settings (user_id) ON DELETE CASCADE,
    type VARCHAR(64) NOT NULL,
    CONSTRAINT uq_notification_email_type UNIQUE (user_id, type)
);

-- ===============
-- Recipient projection: an e-mail request must carry an address, and this
-- context cannot call iam-service while handling a Kafka event.
CREATE TABLE recipient_ref (
    user_id UUID PRIMARY KEY,
    email VARCHAR(255) NOT NULL,
    display_name VARCHAR(255),
    updated_at TIMESTAMP NOT NULL
);

CREATE INDEX idx_recipient_ref_email ON recipient_ref (email);
