# Keycloak Configuration

## How It Works

Keycloak is the identity provider (IdP) for the application. It handles:

- User credentials storage (passwords, enabled/disabled state).
- Token issuance (access tokens + refresh tokens, JWT format).
- Role management (mirrored from the app's IAM service).

> [!IMPORTANT]
>
> The realm JSON export: `keycloak/realm-config/realm-export.json` is automatically imported **on first startup** via
Docker Compose volume mount. You do **not** need to configure Keycloak manually. An existing realm is never
re-imported: to load a changed export locally, remove the `veds_keycloak-postgres-data` volume and start Keycloak again.

## What the Realm Export Creates

| Resource                     | Name                           | Purpose                                                                                            |
|------------------------------|--------------------------------|----------------------------------------------------------------------------------------------------|
| Realm                        | `veds`                         | Application realm                                                                                  |
| Realm roles                  | `USER`, `ADMIN`                | Mapped to Spring Security `ROLE_USER`, `ROLE_ADMIN`                                                |
| Client                       | `veds-api-gateway`             | Confidential client used by the Gateway BFF; standard token exchange enabled                       |
| Client                       | `veds-service-account`         | Service account for IAM backend admin operations                                                   |
| Clients                      | `veds-<name>-service`          | One per service behind the gateway: the audience of a token exchanged for it; no flow of their own |
| Client scopes (optional)     | `veds-<name>-service-audience` | On the gateway client; each adds one service to the `aud` of an exchanged token                    |
| Protocol mapper (predefined) | `roles mapper`                 | Puts realm roles into `realm_access.roles` claim in access token                                   |
| Protocol mapper (predefined) | `email mapper`                 | Puts `email` claim in access token                                                                 |
| Protocol mapper              | `veds-api-gateway-audience`    | Puts `veds-api-gateway` in the `aud` claim of the gateway's own access tokens                      |
| Required action (default)    | `TERMS_AND_CONDITIONS`         | A new account accepts the terms before its first sign-in ends                                      |
| User profile attribute       | `terms_and_conditions`         | When the terms were accepted; admins only                                                          |

The terms text (`termsTitle`, `termsText`, Polish and English) links FastDo's `/terms` and `/privacy-policy` pages, and
Keycloak stores the time of acceptance in `terms_and_conditions`. The attribute is declared in the user profile, because
Keycloak 26 drops attributes the profile does not declare. The realm also sets a password policy (`length(8)`, not the
e-mail, not the username) and offers its pages in Polish and English.

## Authentication Flow — Token Handler (BFF) with Authorization Code + PKCE

**No token of any kind reaches the browser.** The SPA holds one opaque HttpOnly cookie; the gateway keeps the access and
refresh tokens in its session in Redis and, on the way through to a microservice, injects `Authorization: Bearer` with a
token exchanged for that service alone. The OAuth2 work — state, PKCE, the code exchange, the ID token check, the
refresh and the token exchange — is Spring Security's OAuth2 client.

```text
Browser (SPA)              API Gateway (BFF)                Keycloak            Microservices
     |                            |                             |                     |
     |-- 1. GET /auth/authorize ->|                             |                     |
     |                            |-- 302, PKCE challenge ----->|                     |
     |<------------- Keycloak login page (user types password) -|                     |
     |                            |                             |                     |
     |-- 2. GET /auth/callback -->|                             |                     |
     |       ?code&state          |-- 3. code + verifier ------>|                     |
     |                            |<--- access + refresh token -|                     |
     |<-- 4. Set-Cookie: VEDS_SESSION (opaque id, HttpOnly, SameSite=Strict)          |
     |                            |    tokens stored in Redis   |                     |
     |                            |                             |                     |
     |-- 5. GET /projects ------->|                             |                     |
     |       + cookie             |-- 6. token exchange ------->|                     |
     |                            |<--- token, aud = veds-project-service             |
     |                            |-- 7. + Bearer <exchanged JWT> -------------------->|
```

1. **`GET /auth/authorize`** — Spring builds the authorization request (`state`, `nonce`, `code_verifier`), keeps it in
   an encrypted short-lived cookie and redirects to Keycloak with `code_challenge = S256(code_verifier)`.
2. **`GET /auth/callback`** — Spring checks `state`, exchanges the code as a *confidential* client (client secret
   **and** PKCE verifier), validates the ID token and opens a server-side session.
3. **`GET /auth/session`** — the SPA's bootstrap call: "am I logged in, and as whom?". Returns id, e-mail and
   roles, or **`204`** when nobody is. Not being signed in is an answer, not a refusal: a `401` here would be
   indistinguishable from the session store being unreachable, and the SPA would sign the person out over a
   network blip.
   Never a token.
4. **`POST /auth/logout`** — revokes the refresh token at Keycloak and deletes the session.

Each microservice still validates the JWT independently against Keycloak's JWKS endpoint — they are unaware a browser
session ever existed — and accepts only a token whose audience is its own client, `veds-${spring.application.name}`
(`shared-web-config.yml`).

### Each service gets a token for itself alone

The person's own token has the gateway as its only audience, and it never leaves the gateway. Every route carries
`TokenRelay=veds-<name>-service`: the gateway exchanges the person's token at Keycloak (standard token exchange,
RFC 8693) for one whose `aud` is that service alone, and relays that. A token leaked from one service — through a log,
a dump, a bug — opens no other service, and a service cannot replay what it received against its neighbours.

- The exchange asks for `audience=veds-<name>-service` and the optional scope `veds-<name>-service-audience`; the
  realm grants nothing more, so the exchanged token keeps the person's identity and realm roles and gains one audience.
- Exchanged tokens are kept in the session next to the person's tokens and reused until they expire, so a service
  costs one call to Keycloak per five minutes per session, not one per request.
- A caller with its own token (`Authorization: Bearer`, audience `veds-api-gateway`) gets the same exchange on every
  request and no session.

### Why the full Token Handler, not just PKCE plus a token in the response

Handing the SPA an access token — even with the refresh token safely in a cookie — leaves that access token in
JavaScript memory.

|                                           | Token in the SPA       | Token Handler                            |
|-------------------------------------------|------------------------|------------------------------------------|
| XSS can exfiltrate a usable token         | yes                    | no — only an HttpOnly cookie exists      |
| Token in JS memory / `localStorage` / URL | yes                    | no                                       |
| Revoking a session takes effect           | when the token expires | immediately — delete the Redis record    |
| SPA implements a refresh loop             | yes                    | no — the gateway refreshes transparently |

### Implementation notes

**Filter ordering.** `SessionAccessTokenFilter` runs inside Spring Security's chain, after authentication: it turns the
signed-in session into the person's access token, refreshing it when needed, and makes that JWT the request's
authentication. Authorization and the routes therefore see a JWT whether the caller has a session or a token of its
own, and `TokenRelay` exchanges that JWT on the way to the service.

**Session store: Spring Session in Redis, encrypted.** Keycloak's tokens exceed the 4 KB cookie budget once encrypted,
refresh-token rotation would mean rewriting the cookie on every proxied request, and a server-side record is what makes
logout genuinely revoke access. It also lets any gateway replica serve any session. Sessions live under
`veds:session:*`, and every attribute is sealed with AES-GCM (`GATEWAY_SESSION_ENCRYPTION_KEY`) before it reaches
Redis, so a copy of the data reveals no token; a value that no longer decrypts reads as absent and signs that browser
out.

**CSRF.** Once authentication travels in a cookie the browser attaches automatically, CSRF becomes a live concern that a
`Bearer` header did not have. `VEDS_SESSION` is `SameSite=Strict`, so it is never sent on a cross-site request. The pending
authorization request therefore lives in its own cookie, which must be `Lax` — it is read on the callback, which *is* a
cross-site redirect — and is encrypted like the session.

| Cookie                  | SameSite | Lifetime | Why                                                                 |
|-------------------------|----------|----------|---------------------------------------------------------------------|
| `VEDS_SESSION`          | `Strict` | 7 days   | The only cookie the SPA relies on; CSRF defence                     |
| `KEYCLOAK_AUTH_REQUEST` | `Lax`    | 10 min   | State, nonce and PKCE verifier; survives the redirect from Keycloak |

> [!IMPORTANT]
>
> **The gateway has no password grant and no refresh endpoint.** ROPC is removed in OAuth 2.1
> and would require the gateway to receive the user's plaintext password, ruling out MFA,
> WebAuthn and identity brokering, so `directAccessGrantsEnabled` is `false` on the realm
> client. A refresh endpoint would exist only to hand the browser a token — and the browser
> never holds one.

### Refreshing a token is single-flight, per refresh token

The realm sets `revokeRefreshToken: true` with `refreshTokenMaxReuse: 0`, so a refresh token is strictly
single-use. Presenting a spent one is treated as replay and **revokes the whole SSO session** — not just the
rejected request.

That makes concurrent refreshes destructive rather than merely wasteful. A page issuing several API calls at once
would have them all read the same session, all call the token endpoint with the same refresh token, and all but one
replay it — killing the session the winner had just refreshed, roughly every `accessTokenLifespan`.

`SingleFlightRefreshTokenProvider` therefore wraps Spring's refresh: requests of one replica holding the same refresh
token share one refresh, and for thirty seconds a request still holding the old token receives the same result.
`SharedRefreshes` extends that to every replica: the one that claims `veds:refresh-lock:<sha256 of the refresh token>`
calls Keycloak and leaves the new tokens under `veds:refresh-result:<sha256>`, and the others take them from there. A
refresh Keycloak refuses ends the session; when Redis is unreachable a replica refreshes on its own.

> [!WARNING]
> Turning `revokeRefreshToken` off would make the symptom disappear and remove the replay detection that catches a
> stolen refresh token. The lock is the fix; the realm setting is not the problem.

### What lives where

| Concern                                               | Owner           |
|-------------------------------------------------------|-----------------|
| Passwords, sessions, MFA, token issuance, realm roles | **Keycloak**    |
| Acceptance of the terms                               | **Keycloak**    |
| Browser session ↔ token mapping                       | **api-gateway** |
| Profile, settings, role mirror                        | **iam-service** |

Profile data is deliberately *not* stored in Keycloak user attributes: it is not an application database and querying it
is painful. The Admin API stays, in the narrower role of provisioning users at registration and syncing roles.

### Tokens are accepted only by the component they were issued for

Every component requires its own client in the token's `aud` (`spring.security.oauth2.resourceserver.jwt.audiences` in
`shared-web-config.yml`, `veds-${spring.application.name}`): the gateway accepts `veds-api-gateway`, which the gateway
client's audience mapper puts in the person's token, and each service accepts only the token the gateway exchanged for
it. A token Keycloak issued to another client of the realm, `veds-service-account` included, is refused even though its
signature and issuer are valid. Access tokens live five minutes and refresh tokens rotate on
every use (`revokeRefreshToken`, `refreshTokenMaxReuse: 0`), so revoking access needs no deny list.

### Second Factors

Enabling a second factor happens on Keycloak's own pages, reached through the gateway with
`kc_action=CONFIGURE_TOTP`, so no TOTP secret ever passes through iam-service. Disabling has no secret to
handle, so `DELETE /auth/me/security/two-factor` does it directly. Credential types are read from Keycloak
rather than mirrored locally: a copy would be a second answer to the same question, wrong the moment somebody
configures a factor on Keycloak's pages.

Every endpoint under `/auth/me/security` acts on the caller's own account — the subject comes from the token,
never from a path, so there is no way to phrase a request about somebody else.

### How iam-service Learns About a User

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

## Configuration

All Keycloak-related config is centralized in `shared-web/src/main/resources/shared-web-config.yml` and injected
into each service via `KeycloakProperties`.

## Where Do Role Names Live? (Microservices Anti–Shared-Kernel)

Role names are owned by **two places only**:

| Location           | Details                                                                                                                                                                                                                                                                                                        |
|--------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **Keycloak realm** | (`keycloak/realm-config/realm-export.json`) — the runtime source of truth issued in every access token's `realm_access.roles` claim.                                                                                                                                                                           |
| **iam-service**    | (`iam-service/.../domain/model/RoleType.kt`, `internal`) — a type-safe mirror used solely by the role *administrator* (`RoleInitializer` seeds the DB, `AuthService.register` assigns `USER` via the Keycloak Admin API). The enum is `internal` to the iam-service module and intentionally **not** exported. |

> [!NOTE]
>
> Other microservices **do not** depend on iam-service's enum. They check roles as plain strings.

**Why no `shared-web.RoleType` enum:**

| Reason                        | Explanation                                                                                                                              |
|-------------------------------|------------------------------------------------------------------------------------------------------------------------------------------|
| **Shared Kernel Antipattern** | (DDD antipattern, Evans, *DDD* ch. 14) — every change to the role vocabulary would force a coordinated recompile/deploy across services. |
| **Bounded Context Conflict**  | A role's *meaning* (what `ADMIN` is allowed to do) belongs to the service that owns the resource, not to a global enum.                  |
| **Source of Truth Drift**     | The IdP is already the source of truth; an in-code mirror would inevitably drift from Keycloak.                                          |

> [!NOTE]
>
> What stays in `shared-web/security/` is **only** the technical JWT → `Authentication` adapter
(`KeycloakJwtAuthenticationConverter` / `ReactiveKeycloakJwtAuthenticationConverter`). It is role-name-agnostic — it
maps *whatever* strings sit in the configured claim path onto `ROLE_*` authorities. Each service then decides which of
those it cares about, in its own `SecurityConfig`.

## Useful Keycloak URLs (Local Dev)

| URL                                                                | Description                             |
|--------------------------------------------------------------------|-----------------------------------------|
| http://localhost:9000                                              | Keycloak admin console                  |
| http://localhost:9000/realms/veds/.well-known/openid-configuration | OpenID Connect discovery                |
| http://localhost:9000/realms/veds/protocol/openid-connect/certs    | JWKS (public keys for JWT verification) |
| http://localhost:9000/realms/veds/protocol/openid-connect/token    | Token endpoint                          |

## Manual Token Request (curl)

```bash
# Get access token
curl -s -X POST http://localhost:9000/realms/veds/protocol/openid-connect/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=password" \
  -d "client_id=veds-api-gateway" \
  -d "client_secret=KEYCLOAK_GATEWAY_CLIENT_SECRET_HERE" \
  -d "username=test@example.com" \
  -d "password=Test1234!" | jq .
```

## Production TLS

`application-prod.yml` assumes TLS on every hop. Defaults are set for an ingress that terminates TLS in front of the
gateway; flip `SERVER_SSL_ENABLED` when a process holds the certificate itself.

| Leg                       | Setting                                                                                          |
|---------------------------|--------------------------------------------------------------------------------------------------|
| Browser → ingress/gateway | `SERVER_SSL_ENABLED`, `SERVER_SSL_BUNDLE`                                                        |
| Ingress → gateway         | `forward-headers-strategy: framework` (keeps the https scheme in redirect URIs and `Set-Cookie`) |
| Gateway → Redis           | `spring.data.redis.ssl.enabled=true`, `REDIS_SSL_BUNDLE`                                         |
| Service → PostgreSQL      | `DB_SSL_MODE=verify-full`, `DB_SSL_ROOT_CERT`                                                    |
| Service → Kafka           | `KAFKA_SECURITY_PROTOCOL=SASL_SSL`, SCRAM-SHA-512, truststore                                    |
| Cookie flag               | `application.shared.keycloak.cookie.secure: true` (hardcoded, not overridable)                   |

Two choices worth stating:

- **`verify-full`, not `require`, for PostgreSQL.** `require` encrypts but accepts any certificate, so it stops passive
  sniffing and not an active man-in-the-middle.
- **`ssl.endpoint.identification.algorithm: https` for Kafka.** The default in some setups is empty, which disables
  hostname verification and reintroduces the same gap.
- **`forward-headers-strategy` only behind a trusted proxy.** It makes the application believe `X-Forwarded-*`, so it
  must stay off wherever something untrusted can set those headers.
- **`server.ssl.enabled` only when TLS terminates in the process.** With an ingress holding the certificate it stays
  off; turning it on there gives a service talking TLS to a proxy that already did.

The `Secure` cookie flag is fixed to `true` in prod rather than read from an environment variable: a session cookie
without it can be sent over plain http and captured, and that is not a knob worth having.
