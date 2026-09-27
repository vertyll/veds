# iam-service

Users, roles, permissions and the account lifecycle.

| Property  | Value                                                                         |
|-----------|-------------------------------------------------------------------------------|
| Port      | 8082                                                                          |
| Database  | 5432                                                                          |
| Publishes | `user-registered`, `user-profile-updated`; sends the `mail-requested` command |
| Consumes  | —                                                                             |

## Permissions belong to roles

A permission is granted to a role, never to a person. Attaching permissions to users directly is
not RBAC: granting access becomes a list of tick boxes per person, and "what can a manager do"
has no answer. `User.permissions` is derived from roles at read time and never stored.

There are **no** per-user exceptions. Two sources of truth mean an audit has to consult both,
and the administration screen could only ever show half the picture. `User.permissions` is
derived from roles and never stored.

## Credentials

Keycloak is the source of truth for authentication; this service owns the profile and the
authorization model. Password hashes are not stored here.

"No such user" and "wrong password" report the **same** error, `iam.auth.invalid_credentials`.
Telling them apart is a user-enumeration oracle.

## Registration belongs to Keycloak

Signing up, signing in, resetting a password and changing one all happen on Keycloak's own pages,
reached through the gateway. This service learns of a person on their first authenticated call and
provisions them from the token — see [Keycloak](../docs/keycloak.md).

That leaves one answer to every identity question instead of two that can disagree, and no saga:
there is nothing here to undo when a mail fails, because this service sends none.

## Second factor

TOTP is optional and configured on Keycloak's own pages, reached through the gateway with
`kc_action=CONFIGURE_TOTP`. **No TOTP secret ever passes through this service, its logs or the
browser's JavaScript** — there is no second implementation of a security-critical flow to keep
correct.

`GET /auth/me/security` reports the status by asking Keycloak which credentials the user holds.
It is not stored here: a copy would be a second answer to the same question, wrong the moment
somebody configures a factor on Keycloak's pages directly.

Disabling is a normal API call, because it has no secret to handle. The gateway allows only a
fixed set of `kc_action` values — an open one would let a caller push a user into any Keycloak
flow, including ones that change credentials.

## One known mis-signal

`validatePassword` returns `false` for any exception from Keycloak, so an unreachable Keycloak
is reported to the user as a wrong password. It is used when confirming a password change, not
when signing in — sign-in happens on Keycloak's own pages — so the blast radius is small, but
the message is misleading and the cause is invisible in the response.

Fixing it properly means distinguishing "rejected" from "unavailable" in the port's return type.
Left as is deliberately rather than silently: it is a known wart, not an oversight.
