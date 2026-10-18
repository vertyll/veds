-- Project bounded context schema.
--
-- Identifiers are UUIDs assigned by the domain (see Project.kt): an aggregate
-- needs its id before the transaction commits so that outbox events can
-- reference it, and the id crosses service boundaries where a per-database
-- sequence would not be unique.
--
-- There are deliberately no foreign keys to users: user identity is owned by
-- the IAM bounded context and referenced here only by Keycloak subject.

-- ===============
-- Reference data: project types
CREATE TABLE project_type (
    id UUID PRIMARY KEY,
    code VARCHAR(32) NOT NULL UNIQUE,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    version BIGINT
);

CREATE TABLE project_type_translation (
    project_type_id UUID NOT NULL REFERENCES project_type (id) ON DELETE CASCADE,
    language VARCHAR(8) NOT NULL,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    CONSTRAINT uq_project_type_translation UNIQUE (project_type_id, language)
);

-- ===============
-- Reference data: project roles and the permissions they grant
-- A role code is not one of a fixed three: an administrator creates project-scoped
-- roles in iam and this context learns them from role-permissions-changed. The
-- unrestricted flag covers the role that holds everything, including permissions
-- no module has registered yet.
CREATE TABLE project_role (
    id UUID PRIMARY KEY,
    code VARCHAR(64) NOT NULL UNIQUE,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    version BIGINT,
    unrestricted BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE TABLE project_role_permission (
    project_role_id UUID NOT NULL REFERENCES project_role (id) ON DELETE CASCADE,
    permission VARCHAR(128) NOT NULL,
    CONSTRAINT uq_project_role_permission UNIQUE (project_role_id, permission)
);

CREATE TABLE project_role_translation (
    project_role_id UUID NOT NULL REFERENCES project_role (id) ON DELETE CASCADE,
    language VARCHAR(8) NOT NULL,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    CONSTRAINT uq_project_role_translation UNIQUE (project_role_id, language)
);

-- ===============
-- Aggregate root
CREATE TABLE project (
    id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    is_public BOOLEAN NOT NULL DEFAULT FALSE,
    icon_file_id UUID,
    type_id UUID REFERENCES project_type (id) ON DELETE SET NULL,
    owner_id UUID NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    version BIGINT,
    hidden_work_log_enabled BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX idx_project_owner_id ON project (owner_id);
CREATE INDEX idx_project_type_id ON project (type_id);
CREATE INDEX idx_project_is_active ON project (is_active);

-- ===============
-- Per-project categories
CREATE TABLE project_category (
    id UUID PRIMARY KEY,
    project_id UUID NOT NULL REFERENCES project (id) ON DELETE CASCADE,
    color VARCHAR(32) NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    version BIGINT
);

CREATE INDEX idx_project_category_project_id ON project_category (project_id);

CREATE TABLE project_category_translation (
    project_category_id UUID NOT NULL REFERENCES project_category (id) ON DELETE CASCADE,
    language VARCHAR(8) NOT NULL,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    CONSTRAINT uq_project_category_translation UNIQUE (project_category_id, language)
);

-- ===============
-- Per-project workflow statuses
CREATE TABLE project_status (
    id UUID PRIMARY KEY,
    project_id UUID NOT NULL REFERENCES project (id) ON DELETE CASCADE,
    color VARCHAR(32) NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    version BIGINT
);

CREATE INDEX idx_project_status_project_id ON project_status (project_id);

CREATE TABLE project_status_translation (
    project_status_id UUID NOT NULL REFERENCES project_status (id) ON DELETE CASCADE,
    language VARCHAR(8) NOT NULL,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    CONSTRAINT uq_project_status_translation UNIQUE (project_status_id, language)
);

-- ===============
-- Membership: which user holds which role in which project
CREATE TABLE project_member (
    id UUID PRIMARY KEY,
    project_id UUID NOT NULL REFERENCES project (id) ON DELETE CASCADE,
    user_id UUID NOT NULL,
    project_role_id UUID NOT NULL REFERENCES project_role (id),
    assigned_at TIMESTAMP NOT NULL,
    version BIGINT,
    CONSTRAINT uq_project_member_project_user UNIQUE (project_id, user_id)
);

CREATE INDEX idx_project_member_user_id ON project_member (user_id);

-- ===============
-- Invitations awaiting a decision
CREATE TABLE project_invitation (
    id UUID PRIMARY KEY,
    project_id UUID NOT NULL REFERENCES project (id) ON DELETE CASCADE,
    invitee_email VARCHAR(255) NOT NULL,
    invitee_id UUID,
    inviter_id UUID NOT NULL,
    project_role_id UUID NOT NULL REFERENCES project_role (id),
    status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    expires_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    version BIGINT
);

CREATE INDEX idx_project_invitation_project_id ON project_invitation (project_id);
CREATE INDEX idx_project_invitation_email ON project_invitation (invitee_email);
CREATE INDEX idx_project_invitation_status ON project_invitation (status);

-- Only one pending invitation per project and e-mail. Enforced as a partial
-- unique index rather than in application code, so a double-submit cannot
-- create two pending invitations.
CREATE UNIQUE INDEX IF NOT EXISTS uq_project_invitation_pending
    ON project_invitation (project_id, LOWER(invitee_email))
    WHERE status = 'PENDING';

-- ===============
-- Local read model of users, projected from IAM integration events.
-- Never written by this service's own use cases.
CREATE TABLE user_ref (
    user_id UUID PRIMARY KEY,
    email VARCHAR(255) NOT NULL,
    first_name VARCHAR(255),
    last_name VARCHAR(255),
    avatar_file_id UUID,
    updated_at TIMESTAMP NOT NULL
);

CREATE INDEX idx_user_ref_email ON user_ref (email);
