-- Identity: users, roles and the permissions a role grants.
--
-- Keycloak owns credentials and sessions. What is here is the profile and the authorization
-- model, joined to Keycloak by `keycloak_id`; there is no password column and no refresh token,
-- because neither is this service's to hold.
--
-- A permission is granted to a role, never to a person. Attaching permissions directly to users
-- is not RBAC: granting access becomes a list of tick boxes per person, "what can a manager do"
-- has no answer, and nobody can say which people hold a given right. `User.permissions` is
-- derived from roles at read time and never stored — see docs/architecture.md.

-- ===============
-- The person: profile only, joined to Keycloak by keycloak_id
CREATE TABLE "user" (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) NOT NULL,
    first_name VARCHAR(255) NOT NULL,
    last_name VARCHAR(255) NOT NULL,
    phone_number VARCHAR(255) NULL,
    address VARCHAR(500) NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    version BIGINT NULL,
    keycloak_id UUID NULL,
    avatar_file_id UUID NULL,

    CONSTRAINT uk_user_email UNIQUE (email)
);
CREATE INDEX idx_user_created_at ON "user" (created_at);
CREATE UNIQUE INDEX idx_user_keycloak_id ON "user" (keycloak_id);

-- ===============
-- Authorization: roles, the permissions they grant, and who holds them
-- unrestricted is a role that holds everything its modules offer, now and after they
-- grow. Stored as a flag rather than as a full set of rows, so a module shipped
-- tomorrow is covered the day it registers instead of when somebody remembers to tick
-- it. scope says where the role can be held: a GLOBAL role is held platform-wide and
-- lands in the Keycloak token; a PROJECT role is held inside one project and is
-- assigned by project-service.
CREATE TABLE role (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    version BIGINT NULL,
    unrestricted BOOLEAN NOT NULL DEFAULT FALSE,
    scope VARCHAR(16) NOT NULL DEFAULT 'GLOBAL',

    CONSTRAINT uk_role_name UNIQUE (name)
);

CREATE INDEX idx_role_scope ON role (scope);

-- module is the module that enforces the permission. Services register their own
-- catalogue at start-up, so it is written by them and never by hand; it is what lets
-- the administration panel group permissions without knowing in advance which modules
-- exist. scope says where the permission can be held — against a project membership
-- or against the platform, never both: granting a project role the right to edit
-- users, or a platform role the right to comment on a task nobody assigned them, is
-- not a decision an administrator should be able to make by accident.
CREATE TABLE permission (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    version BIGINT NULL,
    module VARCHAR(64) NOT NULL DEFAULT 'admin',
    scope VARCHAR(16) NOT NULL DEFAULT 'PROJECT',

    CONSTRAINT uk_permission_name UNIQUE (name)
);

CREATE INDEX idx_permission_module ON permission (module);

CREATE TABLE user_role_mapping (
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,

    PRIMARY KEY (user_id, role_id),
    CONSTRAINT fk_user_role_mapping_user FOREIGN KEY (user_id) REFERENCES "user"(id) ON DELETE CASCADE,
    CONSTRAINT fk_user_role_mapping_role FOREIGN KEY (role_id) REFERENCES role(id) ON DELETE CASCADE
);

CREATE TABLE role_permission_mapping (
    role_id BIGINT NOT NULL,
    permission_id BIGINT NOT NULL,

    PRIMARY KEY (role_id, permission_id),
    CONSTRAINT fk_role_permission_mapping_role FOREIGN KEY (role_id) REFERENCES role(id) ON DELETE CASCADE,
    CONSTRAINT fk_role_permission_mapping_permission FOREIGN KEY (permission_id) REFERENCES permission(id) ON DELETE CASCADE
);
CREATE INDEX idx_role_permission_mapping_permission ON role_permission_mapping (permission_id);

