package com.vertyll.veds.notification.infrastructure.websocket

import com.vertyll.veds.notification.application.dto.NotificationResponse
import com.vertyll.veds.notification.application.dto.UnreadCountResponse
import com.vertyll.veds.notification.application.port.outbound.NotificationPushPort
import org.slf4j.LoggerFactory
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.stereotype.Component
import java.util.UUID

@Component
internal class StompNotificationPushAdapter(
    private val messagingTemplate: SimpMessagingTemplate,
) : NotificationPushPort {
    private val logger = LoggerFactory.getLogger(javaClass)

    private companion object {
        private const val NOTIFICATIONS_DESTINATION = "/queue/notifications"
        private const val UNREAD_DESTINATION = "/queue/notifications.unread"
    }

    override fun push(
        recipientId: UUID,
        notification: NotificationResponse,
    ) = send(recipientId, NOTIFICATIONS_DESTINATION, notification)

    override fun pushUnreadCount(
        recipientId: UUID,
        unread: Long,
    ) = send(recipientId, UNREAD_DESTINATION, UnreadCountResponse(unread))

    private fun send(
        recipientId: UUID,
        destination: String,
        payload: Any,
    ) {
        try {
            messagingTemplate.convertAndSendToUser(recipientId.toString(), destination, payload)
        } catch (e: Exception) {
            logger.warn("Push to {} failed, notification already persisted: {}", recipientId, e.message)
        }
    }
}
