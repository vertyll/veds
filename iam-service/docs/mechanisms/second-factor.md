# Second factor

How a person enables and disables a second factor without a secret passing through the application.

Enabling a second factor happens on Keycloak's own pages, reached through the gateway with
`kc_action=CONFIGURE_TOTP`, so no TOTP secret ever passes through iam-service. Disabling has no secret to
handle, so `DELETE /auth/me/security/two-factor` does it directly. Credential types are read from Keycloak
rather than mirrored locally: a copy would be a second answer to the same question, wrong the moment somebody
configures a factor on Keycloak's pages.

Every endpoint under `/auth/me/security` acts on the caller's own account — the subject comes from the token,
never from a path, so there is no way to phrase a request about somebody else.
