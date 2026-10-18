package com.vertyll.veds.task.infrastructure.kafka.consumer

import com.vertyll.veds.iam.RolePermissionsChangedEvent
import com.vertyll.veds.shared.messaging.avro.AvroPayloadDeserializer
import com.vertyll.veds.shared.messaging.kafka.ProcessedEventGuard
import com.vertyll.veds.sharedauthz.PermissionCatalogue
import com.vertyll.veds.task.domain.model.RolePermissionsRef
import com.vertyll.veds.task.domain.repository.RolePermissionsRepository
import com.vertyll.veds.task.infrastructure.kafka.TaskKafkaTopics
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.messaging.handler.annotation.Header
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
internal class RolePermissionsConsumerAdapter(
    private val avroPayloadDeserializer: AvroPayloadDeserializer,
    private val rolePermissions: RolePermissionsRepository,
    private val catalogue: PermissionCatalogue,
    private val processedEventGuard: ProcessedEventGuard,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    private companion object {
        const val GROUP_PREFIX = "task-service:"
        const val PROJECT_SCOPE = "PROJECT"
    }

    @KafkaListener(topics = [TaskKafkaTopics.Consumed.ROLE_PERMISSIONS_CHANGED])
    @Transactional
    fun onRolePermissionsChanged(
        @Payload payload: ByteArray,
        @Header(name = "eventId", required = false) eventId: String?,
    ) {
        val topic = TaskKafkaTopics.Consumed.ROLE_PERMISSIONS_CHANGED
        if (eventId != null && !processedEventGuard.claim(eventId, GROUP_PREFIX + topic)) {
            logger.debug("Already handled {} from {}", eventId, topic)
            return
        }

        val event = avroPayloadDeserializer.deserialize(topic, payload) as RolePermissionsChangedEvent

        if (event.scope.toString() != PROJECT_SCOPE) {
            logger.debug("Ignoring {} role {}: task access follows project membership", event.scope, event.role)
            return
        }

        if (event.removed) {
            rolePermissions.deleteByName(event.role.toString())
            logger.info("Dropped the permission projection for role {}", event.role)
            return
        }

        rolePermissions.save(
            RolePermissionsRef(
                roleName = event.role.toString(),
                permissions = event.permissions.map { it.toString() }.intersect(catalogue.names),
                unrestricted = event.unrestricted,
            ),
        )
    }
}
