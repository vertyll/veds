# Keycloak Configuration

## How It Works

Keycloak is the identity provider (IdP) for the application. It handles:
- User credentials storage (passwords, enabled/disabled state).
- Token issuance (access tokens + refresh tokens, JWT format).
- Role management (mirrored from the app's IAM service).

> **Note:** The realm JSON export: `keycloak/realm-config/realm-export.json` is automatically imported **on first startup** via Docker Compose volume mount. You do **not** need to configure Keycloak manually.

## What the Realm Export Creates

| Resource                     | Name                   | Purpose                                                          |
|------------------------------|------------------------|------------------------------------------------------------------|
| Realm                        | `veds`                 | Application realm                                                |
| Realm roles                  | `USER`, `ADMIN`        | Mapped to Spring Security `ROLE_USER`, `ROLE_ADMIN`              |
| Client                       | `veds-api-gateway`     | Confidential client used by the Gateway BFF                      |
| Client                       | `veds-service-account` | Service account for IAM backend admin operations                 |
| Protocol mapper (predefined) | `roles mapper`         | Puts realm roles into `realm_access.roles` claim in access token |
| Protocol mapper (predefined) | `email mapper`         | Puts `email` claim in access token                               |

## Authentication Flow

```
Frontend ──► API Gateway (BFF) ──► Keycloak
                │
                ├── POST /auth/token          → login, returns accessToken in body + refreshToken in HttpOnly cookie
                ├── POST /auth/refresh-token  → reads cookie, refreshes tokens
                └── POST /auth/logout         → invalidates token, clears cookie

Frontend ──► API Gateway ──► Microservices (Bearer token)
                │
                └── Authorization: Bearer <accessToken>
                    Each microservice validates JWT independently via Keycloak's JWKS endpoint
```

## Configuration

All Keycloak-related config is centralized in `shared-infrastructure/src/main/resources/shared-config.yml` and injected into each service via `KeycloakProperties`.

## Where Do Role Names Live? (Microservices Anti–Shared-Kernel)

Role names are owned by **two places only**:
1. **Keycloak realm** (`keycloak/realm-config/realm-export.json`) — the runtime source of truth issued in every access token's `realm_access.roles` claim.
2. **iam-service** (`iam-service/.../domain/model/RoleType.kt`, `internal`) — a type-safe mirror used solely by the role *administrator* (`RoleInitializer` seeds the DB, `AuthService.register` assigns `USER` via the Keycloak Admin API). The enum is `internal` to the iam-service module and intentionally **not** exported.

> **Note** Other microservices **do not** depend on iam-service's enum. They check roles as plain strings.

**Why no `shared-infrastructure.RoleType` enum:**
- It would be a **Shared Kernel** (DDD antipattern, Evans, *DDD* ch. 14) — every change to the role vocabulary would force a coordinated recompile/deploy across services.
- It would conflict with the **Bounded Context** boundary: a role's *meaning* (what `ADMIN` is allowed to do) belongs to the service that owns the resource, not to a global enum.
- The IdP is already the source of truth; an in-code mirror would inevitably drift from Keycloak.

What stays in `shared-infrastructure/security/` is **only** the technical JWT → `Authentication` adapter (`KeycloakJwtAuthenticationConverter` / `ReactiveKeycloakJwtAuthenticationConverter`). It is role-name-agnostic — it maps *whatever* strings sit in the configured claim path onto `ROLE_*` authorities. Each service then decides which of those it cares about, in its own `SecurityConfig`.

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
