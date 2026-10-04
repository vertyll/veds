# api-gateway

The single entry point, and the only component that holds tokens.

| Property      | Value            |
|---------------|------------------|
| Port          | 8080             |
| Session store | Redis, encrypted |

## Token Handler

The browser never sees an access token. It holds an opaque session id in an HttpOnly cookie; the
gateway keeps the person's tokens in that session, refreshes them transparently, and gives each
service a token exchanged for it alone.

That removes the entire class of token-in-JavaScript problems: nothing to exfiltrate via XSS, no
refresh loop in the client, no token in a query string.

- **Authorization Code with PKCE**, through Spring Security's OAuth2 client, not ROPC. The
  password grant is deprecated and would put credentials through the front end.
- **One audience per service.** Every route has `TokenRelay=veds-<name>-service`: the person's
  token is exchanged at Keycloak for one whose `aud` is that service alone, and kept in the
  session until it expires.
- **Sessions are AES-256-GCM encrypted in Redis** (Spring Session, `veds:session:*`), with a
  random nonce per write. The key is mandatory in production, so a misconfigured deployment fails
  to start rather than storing tokens in the clear.
- **An undecryptable session is treated as absent**, not as an error: a rotated key should log
  people out, not return 500 to everybody.
- **One refresh per refresh token**, across replicas too (`veds:refresh-lock:*`,
  `veds:refresh-result:*`).

## Filter ordering

`SessionAccessTokenFilter` runs inside Spring Security's chain, after authentication: it turns the
session into the person's JWT, so authorization and `TokenRelay` see the same thing whether the
caller has a session or a bearer token of its own.

WebSocket upgrades go through the same route filter, which is why notification-service can
authenticate its handshake from a normal header.

See [Keycloak](../docs/keycloak.md).
