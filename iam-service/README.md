# iam-service

Users, roles, permissions and the account lifecycle.

| Property  | Value                                                                         |
|-----------|-------------------------------------------------------------------------------|
| Port      | 8082                                                                          |
| Database  | 5432                                                                          |
| Publishes | `user-registered`, `user-profile-updated`; sends the `mail-requested` command |
| Consumes  | —                                                                             |

## Permissions belong to roles

A permission is granted to a role, never to a person, and there are no per-user exceptions; why is in
[Architecture](../docs/architecture.md#permissions-belong-to-roles).

## Credentials

Keycloak is the source of truth for authentication; this service owns the profile and the
authorization model. Password hashes are not stored here.

## Registration belongs to Keycloak

Signing up, signing in, resetting a password and changing one all happen on Keycloak's own pages,
reached through the gateway. This service learns of a person on their first authenticated call and
provisions them from the token — see [Keycloak](../docs/keycloak.md).

That leaves one answer to every identity question instead of two that can disagree, and no saga:
there is nothing here to undo when a mail fails, because this service sends none.

## Second factor

Configured on Keycloak's pages, never through this service; how enabling, disabling and reading the status work is in
[Keycloak](../docs/keycloak.md#second-factors).
