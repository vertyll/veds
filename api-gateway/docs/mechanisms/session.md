# Session

How the gateway turns an opaque cookie into the person's token, and keeps that safe.

**Filter ordering.** `SessionAccessTokenFilter` runs inside Spring Security's chain, after authentication: it turns the
signed-in session into the person's access token, refreshing it when needed, and makes that JWT the request's
authentication. Authorization and the routes therefore see a JWT whether the caller has a session or a token of its
own, and `TokenRelay` exchanges that JWT on the way to the service. WebSocket upgrades go through the same route
filter, which is why notification-service can authenticate its handshake from a normal header.

**Session store: Spring Session in Redis, encrypted.** Keycloak's tokens exceed the 4 KB cookie budget once encrypted,
refresh-token rotation would mean rewriting the cookie on every proxied request, and a server-side record is what makes
logout genuinely revoke access. It also lets any gateway replica serve any session. Sessions live under
`veds:session:*`, and every attribute is sealed with AES-256-GCM, with a random nonce per write
(`GATEWAY_SESSION_ENCRYPTION_KEY`, mandatory in production, so a misconfigured deployment fails to start rather than
storing tokens in the clear) before it reaches Redis, so a copy of the data reveals no token; a value that no longer
decrypts reads as absent and signs that browser out.

**CSRF.** Once authentication travels in a cookie the browser attaches automatically, CSRF becomes a live concern that a
`Bearer` header did not have. `VEDS_SESSION` is `SameSite=Strict`, so it is never sent on a cross-site request. The
pending authorization request therefore lives in its own cookie, which must be `Lax` — it is read on the callback, which
*is* a cross-site redirect — and is encrypted like the session.

| Cookie                  | SameSite | Lifetime | Why                                                                 |
|-------------------------|----------|----------|---------------------------------------------------------------------|
| `VEDS_SESSION`          | `Strict` | 7 days   | The only cookie the SPA relies on; CSRF defense                     |
| `KEYCLOAK_AUTH_REQUEST` | `Lax`    | 10 min   | State, nonce and PKCE verifier; survives the redirect from Keycloak |

> [!IMPORTANT]
>
> **The gateway has no password grant and no refresh endpoint.** ROPC is removed in OAuth 2.1
> and would require the gateway to receive the user's plaintext password, ruling out MFA,
> WebAuthn and identity brokering, so `directAccessGrantsEnabled` is `false` on the realm
> client. A refresh endpoint would exist only to hand the browser a token — and the browser
> never holds one.
