# User provisioning

How iam-service learns about a person Keycloak already knows.

Keycloak owns registration, so a user can reach the system without iam-service ever having heard of them — an
identity created in the admin console, imported with the realm, or federated from another provider. iam-service
therefore provisions the local user on first contact: `GET /auth/me` calls `ProvisionCurrentUserUseCase`, which
records the identity from the JWT claims, assigns the default `USER` role, and publishes `user-registered` so the
other services build their `user_ref` projections exactly as they would after a registration.

Two consequences follow:

| Consequence                                                            | Why                                                                            |
|------------------------------------------------------------------------|--------------------------------------------------------------------------------|
| A user exists in iam-service only after their first authenticated call | Nothing observes Keycloak; the identity arrives on the request that carries it |
| `GET /auth/me` writes                                                  | It is the session-establishment call, so it is where first contact happens     |

Provisioning is idempotent: an identity already known to iam-service is left untouched and announces nothing. A
missing default role is refused rather than provisioned around — a role-less account looks signed in and silently
cannot do anything.
