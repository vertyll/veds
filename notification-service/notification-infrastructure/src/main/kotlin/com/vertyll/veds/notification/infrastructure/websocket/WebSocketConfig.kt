package com.vertyll.veds.notification.infrastructure.websocket

import org.springframework.context.annotation.Configuration
import org.springframework.messaging.simp.config.MessageBrokerRegistry
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker
import org.springframework.web.socket.config.annotation.StompEndpointRegistry
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer

@Configuration
@EnableWebSocketMessageBroker
internal class WebSocketConfig(
    private val principalHandshakeHandler: PrincipalHandshakeHandler,
) : WebSocketMessageBrokerConfigurer {
    override fun registerStompEndpoints(registry: StompEndpointRegistry) {
        registry
            .addEndpoint(ENDPOINT)
            // The gateway refuses a handshake from an origin it does not allow; here every handshake already carries
            // the bearer token the gateway attached, which no page of another origin can attach itself.
            .setAllowedOriginPatterns("*")
            .setHandshakeHandler(principalHandshakeHandler)
    }

    override fun configureMessageBroker(registry: MessageBrokerRegistry) {
        registry.enableSimpleBroker(QUEUE_PREFIX)
        registry.setUserDestinationPrefix(USER_DESTINATION_PREFIX)
    }

    private companion object {
        private const val ENDPOINT = "/ws/notifications"
        private const val USER_DESTINATION_PREFIX = "/user"

        private const val QUEUE_PREFIX = "/queue"
    }
}
