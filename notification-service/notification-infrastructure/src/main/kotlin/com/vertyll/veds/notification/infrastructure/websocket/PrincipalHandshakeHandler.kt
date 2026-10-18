package com.vertyll.veds.notification.infrastructure.websocket

import org.slf4j.LoggerFactory
import org.springframework.http.server.ServerHttpRequest
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import org.springframework.stereotype.Component
import org.springframework.web.socket.WebSocketHandler
import org.springframework.web.socket.server.support.DefaultHandshakeHandler
import java.security.Principal

@Component
internal class PrincipalHandshakeHandler : DefaultHandshakeHandler() {
    private val log = LoggerFactory.getLogger(javaClass)

    override fun determineUser(
        request: ServerHttpRequest,
        wsHandler: WebSocketHandler,
        attributes: MutableMap<String, Any>,
    ): Principal? {
        val authentication = request.principal
        if (authentication !is JwtAuthenticationToken) {
            log.warn("Refusing handshake: no validated JWT on the upgrade request")
            return null
        }

        val subject = authentication.token.subject
        if (subject.isNullOrBlank()) {
            log.warn("Refusing handshake: token carries no subject claim")
            return null
        }

        return StompPrincipal(subject)
    }
}
