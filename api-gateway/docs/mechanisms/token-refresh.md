# Token refresh

How the session keeps a valid access token without signing the person out when requests race.

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
>
> Turning `revokeRefreshToken` off would make the symptom disappear and remove the replay detection that catches a
> stolen refresh token. The lock is the fix; the realm setting is not the problem.
