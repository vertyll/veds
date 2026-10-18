package com.vertyll.veds.iam.infrastructure.kafka.producer

import com.vertyll.veds.iam.RolePermissionsChangedEvent
import com.vertyll.veds.iam.application.port.outbound.RolePermissionsEventPublisherPort
import com.vertyll.veds.iam.domain.model.Role
import com.vertyll.veds.iam.domain.model.RoleScope
import com.vertyll.veds.iam.infrastructure.kafka.IamKafkaTopics
import com.vertyll.veds.shared.messaging.avro.AvroPayloadSerializer
import com.vertyll.veds.shared.messaging.event.Events
import com.vertyll.veds.shared.messaging.kafka.KafkaOutboxProcessor
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
internal class KafkaRolePermissionsEventPublisherAdapter(
    private val kafkaOutboxProcessor: KafkaOutboxProcessor,
    private val avroPayloadSerializer: AvroPayloadSerializer,
) : RolePermissionsEventPublisherPort {
    private val logger = LoggerFactory.getLogger(javaClass)

    override fun publishChanged(role: Role) {
        enqueue(
            role = role.name,
            scope = role.scope,
            removed = false,
            unrestricted = role.unrestricted,
            permissions = role.permissions.map { it.name }.sorted(),
        )
    }

    override fun publishRemoved(
        roleName: String,
        scope: RoleScope,
    ) {
        enqueue(role = roleName, scope = scope, removed = true, unrestricted = false, permissions = emptyList())
    }

    private fun enqueue(
        role: String,
        scope: RoleScope,
        removed: Boolean,
        unrestricted: Boolean,
        permissions: List<String>,
    ) {
        val eventId = Events.newId()
        val event =
            RolePermissionsChangedEvent
                .newBuilder()
                .setEventId(eventId)
                .setTimestamp(Events.now())
                .setRole(role)
                .setScope(scope.name)
                .setRemoved(removed)
                .setUnrestricted(unrestricted)
                .setPermissions(permissions)
                .build()
        kafkaOutboxProcessor.saveOutboxMessage(
            topic = IamKafkaTopics.ROLE_PERMISSIONS_CHANGED,
            key = role,
            payload = avroPayloadSerializer.serialize(IamKafkaTopics.ROLE_PERMISSIONS_CHANGED, event),
            sagaId = null,
            eventId = eventId,
        )
        logger.info("Saved role permissions for {} to outbox ({} permissions)", role, permissions.size)
    }
}
