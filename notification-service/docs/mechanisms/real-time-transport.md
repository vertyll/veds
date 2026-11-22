# Real-time transport

How a new notification reaches an open browser.

STOMP over WebSocket at `/ws/notifications`, user destinations `/user/queue/notifications` and
`/user/queue/notifications.unread`.

- **The simple in-memory broker is deliberate.** Fan-out already happens through Kafka, so every
  replica sees every event and pushes only to the sessions it holds. An external relay would add
  a second fan-out mechanism beside the one that works.
- **The handshake authenticates from the `Authorization` header**, like every other request: it
  is an ordinary HTTP upgrade, and the gateway swaps the session cookie for a token before
  routing. The usual `?token=` workaround is unnecessary — the browser holds no token — and
  would be worse, since a token in a URL lands in access logs.
- **The gateway checks the handshake's `Origin`, not this service.** A browser opens a WebSocket
  without CORS, but sends the session cookie and the page's `Origin` with it, so a page of
  another origin could otherwise open a socket as the signed-in reader (RFC 6455, 10.2). The
  gateway, which holds the cookie and the list of allowed origins, refuses a handshake from any
  other origin with `403`, as the [session](../../../api-gateway/docs/mechanisms/session.md)
  describes. This service accepts every
  origin, since it authenticates by a bearer token that no page can attach to a WebSocket.
- **Pushes are the best effort.** A broker failure is logged and swallowed: the record is already
  committed and the recipient sees it on their next load. Propagating would roll back a
  notification that was correctly raised.
