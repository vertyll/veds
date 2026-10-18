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
    companion object {
        const val ENDPOINT = "/ws/notifications"
        const val USER_DESTINATION_PREFIX = "/user"

        const val QUEUE_PREFIX = "/queue"
    }

    override fun registerStompEndpoints(registry: StompEndpointRegistry) {
        registry
            .addEndpoint(ENDPOINT)
            .setAllowedOriginPatterns("*")
            .setHandshakeHandler(principalHandshakeHandler)
    }

    override fun configureMessageBroker(registry: MessageBrokerRegistry) {
        registry.enableSimpleBroker(QUEUE_PREFIX)
        registry.setUserDestinationPrefix(USER_DESTINATION_PREFIX)
    }
}
