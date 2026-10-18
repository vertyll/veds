-- Task bounded context schema.
--
-- Identifiers are UUIDs assigned by the domain: an aggregate needs its id before
-- the transaction commits so outbox events can reference it, and the id crosses
-- service boundaries where a per-database sequence would not be unique.
--
-- There are deliberately no foreign keys to projects, categories, statuses or
-- users. Those rows live in other services' databases; what this schema holds
-- are *projections* of them, refreshed by integration events.

-- ===============
-- Aggregate root
CREATE TABLE task (
    id UUID PRIMARY KEY,
    project_id UUID NOT NULL,
    number INTEGER NOT NULL,
    name TEXT NOT NULL,
    description TEXT,
    price_estimation INTEGER NOT NULL DEFAULT 0,
    worked_minutes INTEGER NOT NULL DEFAULT 0,
    priority VARCHAR(16) NOT NULL DEFAULT 'MEDIUM',
    status_id UUID,
    access_role_id UUID,
    created_by UUID NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    version BIGINT,
    CONSTRAINT chk_task_price_estimation CHECK (price_estimation >= 0),
    CONSTRAINT chk_task_worked_minutes CHECK (worked_minutes >= 0),
    CONSTRAINT uq_task_project_number UNIQUE (project_id, number)
);

CREATE INDEX idx_task_status_id ON task (status_id);
CREATE INDEX idx_task_created_by ON task (created_by);
CREATE INDEX idx_task_is_active ON task (is_active);

-- The board is always filtered by project and almost always by active-only,
-- so the composite index is what the list query actually uses.
CREATE INDEX idx_task_board ON task (project_id, is_active, created_at DESC);

-- ===============
-- Element collections: ids owned elsewhere, so no foreign key to point at
CREATE TABLE task_category (
    task_id UUID NOT NULL REFERENCES task (id) ON DELETE CASCADE,
    category_id UUID NOT NULL,
    CONSTRAINT uq_task_category UNIQUE (task_id, category_id)
);

CREATE INDEX idx_task_category_category ON task_category (category_id);

CREATE TABLE task_assignee (
    task_id UUID NOT NULL REFERENCES task (id) ON DELETE CASCADE,
    user_id UUID NOT NULL,
    CONSTRAINT uq_task_assignee UNIQUE (task_id, user_id)
);

CREATE INDEX idx_task_assignee_user ON task_assignee (user_id);

-- Files attached to a task itself, as opposed to a comment. Ids only, and no
-- foreign key: the file records live in file-service's database. What keeps the
-- two in step is the `file-deleted` event, which removes the reference here
-- rather than leaving a broken download behind.
CREATE TABLE task_attachment (
    task_id UUID NOT NULL REFERENCES task (id) ON DELETE CASCADE,
    attachment_id UUID NOT NULL,
    CONSTRAINT uq_task_attachment UNIQUE (task_id, attachment_id)
);

-- Used when a file is deleted and every task referencing it has to be repaired.
CREATE INDEX idx_task_attachment_attachment ON task_attachment (attachment_id);

-- ===============
-- Comments
CREATE TABLE task_comment (
    id UUID PRIMARY KEY,
    task_id UUID NOT NULL REFERENCES task (id) ON DELETE CASCADE,
    author_id UUID NOT NULL,
    content TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    version BIGINT
);

CREATE INDEX idx_task_comment_task ON task_comment (task_id);
CREATE INDEX idx_task_comment_author ON task_comment (author_id);

CREATE TABLE task_comment_attachment (
    comment_id UUID NOT NULL REFERENCES task_comment (id) ON DELETE CASCADE,
    attachment_id UUID NOT NULL,
    CONSTRAINT uq_task_comment_attachment UNIQUE (comment_id, attachment_id)
);

-- ===============
-- Work log. task.worked_minutes holds the sum of these entries; it is never
-- typed in by hand, so the two cannot disagree. A hidden entry is readable by
-- its author and by whoever holds the permission to read hidden entries;
-- entries are visible by default.
CREATE TABLE task_work_log (
    id UUID PRIMARY KEY,
    task_id UUID NOT NULL REFERENCES task (id) ON DELETE CASCADE,
    author_id UUID NOT NULL,
    minutes INTEGER NOT NULL,
    worked_on DATE NOT NULL,
    description TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    version BIGINT NOT NULL DEFAULT 0,
    hidden BOOLEAN NOT NULL DEFAULT FALSE,

    CONSTRAINT chk_work_log_minutes CHECK (minutes > 0 AND minutes <= 1440)
);

CREATE INDEX idx_task_work_log_task_id ON task_work_log (task_id, worked_on);
CREATE INDEX idx_task_work_log_author_id ON task_work_log (author_id);

-- ===============
-- Read models projected from project-service and iam-service events.
-- Never written by this service's own use cases.
CREATE TABLE project_ref (
    project_id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    updated_at TIMESTAMP NOT NULL,
    hidden_work_log_enabled BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE TABLE project_category_ref (
    category_id UUID PRIMARY KEY,
    project_id UUID NOT NULL,
    color VARCHAR(32) NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE INDEX idx_category_ref_project ON project_category_ref (project_id);

-- Every supported language arrives on the event, so the board never has to fall
-- back to another language when rendering a label.
CREATE TABLE project_category_ref_name (
    category_id UUID NOT NULL REFERENCES project_category_ref (category_id) ON DELETE CASCADE,
    language VARCHAR(8) NOT NULL,
    name VARCHAR(255) NOT NULL,
    CONSTRAINT uq_category_ref_name UNIQUE (category_id, language)
);

CREATE TABLE project_status_ref (
    status_id UUID PRIMARY KEY,
    project_id UUID NOT NULL,
    color VARCHAR(32) NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE INDEX idx_status_ref_project ON project_status_ref (project_id);

CREATE TABLE project_status_ref_name (
    status_id UUID NOT NULL REFERENCES project_status_ref (status_id) ON DELETE CASCADE,
    language VARCHAR(8) NOT NULL,
    name VARCHAR(255) NOT NULL,
    CONSTRAINT uq_status_ref_name UNIQUE (status_id, language)
);

-- Membership projection, read on every authorization decision.
CREATE TABLE project_membership_ref (
    project_id UUID NOT NULL,
    user_id UUID NOT NULL,
    role_code VARCHAR(32) NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    PRIMARY KEY (project_id, user_id)
);

CREATE INDEX idx_membership_ref_user ON project_membership_ref (user_id);

CREATE TABLE user_ref (
    user_id UUID PRIMARY KEY,
    email VARCHAR(255) NOT NULL,
    first_name VARCHAR(255),
    last_name VARCHAR(255),
    avatar_file_id UUID,
    updated_at TIMESTAMP NOT NULL
);

-- What each role grants, as announced by iam-service. A local copy so an
-- authorization decision costs no call and survives iam being down; the role
-- name is the key because that is what a membership and a token both carry.
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
