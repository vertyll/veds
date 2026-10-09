# api-gateway

The single entry point, and the only component that holds tokens.

| Property      | Value            |
|---------------|------------------|
| Port          | 8080             |
| Session store | Redis, encrypted |

## Token handler

The browser never sees an access token. It holds an opaque session id in an HttpOnly cookie; the gateway keeps the
person's tokens in that session, refreshes them transparently, and gives each service a token exchanged for it alone.
That removes the entire class of token-in-JavaScript problems: nothing to exfiltrate via XSS, no refresh loop in the
client, no token in a query string. The whole sign-in flow is in [Keycloak](../docs/keycloak.md).

## Mechanisms

- [Session](docs/mechanisms/session.md) – How the gateway turns an opaque cookie into the person's token, and keeps that
  safe.
- [Token exchange](docs/mechanisms/token-exchange.md) – How each service receives a token meant for it alone, and why a
  token leaked from one opens no other.
- [Token refresh](docs/mechanisms/token-refresh.md) – How the session keeps a valid access token without signing the
  person out when requests race.
