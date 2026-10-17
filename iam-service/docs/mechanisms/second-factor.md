# Second factor

How a person enables and disables a second factor without a secret passing through the application.

Both happen on Keycloak's own pages, reached through the gateway's `/auth/authorize` with a `kc_action`:
`CONFIGURE_TOTP` sets the second factor up, and `delete_credential:<id>` removes it after Keycloak asks the person to
confirm, signing them in again when their sign-in is old. No TOTP secret ever passes through iam-service, and a
session taken over in the browser cannot turn the second factor off without that confirmation. The gateway passes on
`delete_credential` only with a UUID, and Keycloak removes a credential only when it is the person's own.

`GET /auth/me/security` tells FastDo whether the second factor is on and names the one-time-password credential the
removal needs. Credential types are read from Keycloak rather than mirrored locally: a copy would be a second answer
to the same question, wrong the moment somebody configures a factor on Keycloak's pages.

Every endpoint under `/auth/me/security` acts on the caller's own account — the subject comes from the token,
never from a path, so there is no way to phrase a request about somebody else.
