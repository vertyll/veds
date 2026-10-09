# Token exchange

How each service receives a token meant for it alone, and why a token leaked from one opens no other.

The person's own token has the gateway as its only audience, and it never leaves the gateway. Every route carries
`TokenRelay=veds-<name>-service`: the gateway exchanges the person's token at Keycloak (standard token exchange,
RFC 8693) for one whose `aud` is that service alone, and relays that. A token leaked from one service — through a log,
a dump, a bug — opens no other service, and a service cannot replay what it received against its neighbors.

- The exchange asks for `audience=veds-<name>-service` and the optional scope `veds-<name>-service-audience`; the
  realm grants nothing more, so the exchanged token keeps the person's identity and realm roles and gains one audience.
- Exchanged tokens are kept in the session next to the person's tokens and reused until they expire, so a service
  costs one call to Keycloak per five minutes per session, not one per request.
- A caller with its own token (`Authorization: Bearer`, audience `veds-api-gateway`) gets the same exchange on every
  request and no session.

## Tokens are accepted only by the component they were issued for

Every component requires its own client in the token's `aud` (`spring.security.oauth2.resourceserver.jwt.audiences` in
`shared-web-config.yml`, `veds-${spring.application.name}`): the gateway accepts `veds-api-gateway`, which the gateway
client's audience mapper puts in the person's token, and each service accepts only the token the gateway exchanged for
it. A token Keycloak issued to another client of the realm, `veds-service-account` included, is refused even though its
signature and issuer are valid. Access tokens live five minutes and refresh tokens rotate on
every use (`revokeRefreshToken`, `refreshTokenMaxReuse: 0`), so revoking access needs no deny list.
