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
`veds:session:*`, and every attribute is written as JSON, with Spring Security's Jackson modules for the security
context and the OAuth2 types, and sealed with AES-256-GCM, with a random nonce per write
(`GATEWAY_SESSION_ENCRYPTION_KEY`, mandatory in production, so a misconfigured deployment fails to start rather than
storing tokens in the clear) before it reaches Redis, so a copy of the data reveals no token. JSON rather than Java
serialization: reading Java-serialized bytes runs code the bytes choose, and ties a session to class versions. A value
that no longer decrypts, or is not such JSON, reads as absent; a session whose own fields read as absent is deleted, and
that browser signs in again.

**Calls to Keycloak are bounded.** Token requests, the signing keys and the logout call share one `WebClient` on Boot's
connector, with the connect and read timeouts of `spring.http.clients` (5 s and 10 s); routes to the services use
`spring.cloud.gateway.server.webflux.httpclient` (5 s to connect, 30 s for a response), and Redis 2 s.

**CSRF.** Once authentication travels in a cookie the browser attaches automatically, CSRF becomes a live concern that a
`Bearer` header did not have. The session cookie is `SameSite=Strict`, so the browser does not send it on a request
from another site, but that is defense in depth, not the defense: FastDo and the gateway are different origins of one
site, and `SameSite` does nothing against a sibling subdomain. Every write the session authenticates therefore carries
a CSRF token, Spring Security's own: the token lives in a cookie, and a `POST`, `PUT`, `PATCH` or `DELETE` must send it
back in the `X-XSRF-TOKEN` header, or it is refused with a `403` problem document before it reaches a service. Its code,
`common.csrf_token_invalid`, is not the `common.access_denied` of other refusals, so FastDo knows that a fresh token
fixes it: it asks `GET /auth/session` for one and sends the write once more.

FastDo cannot read a cookie of the gateway's origin, so the gateway hands the token over in the `X-XSRF-TOKEN` header of
`GET /auth/session`, the call FastDo starts with, and lets CORS expose it; the token is masked anew in each response, so
a compressed response does not reveal it. The token cookie can then stay `HttpOnly`, and carries the `__Host-` prefix,
so a sibling subdomain cannot plant a token of its own choosing.

A request with `Authorization: Bearer` needs no token: a browser never sets that header on its own, and another site
cannot set it without a preflight the gateway refuses. The bearer token is found as the resource server finds it, so
only the requests it authenticates are exempt; any other scheme is still authenticated by the session cookie, and still
needs the token.

A WebSocket handshake is a `GET`, which needs no CSRF token, and the browser opens it without a CORS preflight while
still sending the session cookie, so a page of another origin could open the notification socket as the signed-in
reader (RFC 6455, 10.2). The browser does send the page's `Origin`, though, and the gateway checks it twice against
`application.gateway.cors.allowed-origins`: the CORS filter refuses any request from another origin with `403`, the
handshake included, before the session is read; and Spring Cloud Gateway's WebSocket routing refuses a handshake from
any origin but the gateway's own unless it is listed in `spring.cloud.gateway.server.webflux.websocket.allowed-origins`,
which `application.yml` binds to the same setting. That is why the setting is one comma-separated value: a list could
not be read through the placeholder, and without it FastDo's own socket would be refused. The service behind the
gateway does not check `Origin` again: it sees only the bearer token the gateway attached.

The pending authorization request lives in its own cookie, which must be `Lax`: it is read on the callback, which *is*
a cross-site redirect, and is encrypted like the session.

| Cookie                  | SameSite | Lifetime                   | Why                                                                 |
|-------------------------|----------|----------------------------|---------------------------------------------------------------------|
| `__Host-VEDS_SESSION`   | `Strict` | browser session, 10 h idle | The session id, the only cookie that authenticates                  |
| `__Host-XSRF-TOKEN`     | `Strict` | browser session            | The CSRF token a session write sends back in `X-XSRF-TOKEN`         |
| `KEYCLOAK_AUTH_REQUEST` | `Lax`    | 10 min                     | State, nonce and PKCE verifier; survives the redirect from Keycloak |

The session lasts as long as the one Keycloak keeps for the realm: 10 hours, after which the refresh token it holds is
refused and the session ends anyway, so neither the record in Redis nor the cookie outlives it. The cookie has no
`Max-Age`: a session cookie in the sense of RFC 6265bis, which the browser drops when it closes.

In production the session and CSRF cookies carry the `__Host-` prefix, so the browser accepts them only with `Secure`,
`Path=/` and no `Domain`. Locally, over plain HTTP, they are not `Secure`, and are named `VEDS_SESSION` and
`XSRF-TOKEN`, since a prefixed cookie without `Secure` would be refused. All three cookies take `Secure` from Spring
Boot's own `server.reactive.session.cookie.secure`: `true` in `application.yml`, `false` in `application-local.yml`.

> [!IMPORTANT]
>
> **The gateway has no password grant and no refresh endpoint.** ROPC is removed in OAuth 2.1
> and would require the gateway to receive the user's plaintext password, ruling out MFA,
> WebAuthn and identity brokering, so `directAccessGrantsEnabled` is `false` on the realm
> client. A refresh endpoint would exist only to hand the browser a token — and the browser
> never holds one.
