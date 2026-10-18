package com.vertyll.veds.translation.infrastructure.kafka

import com.vertyll.veds.iam.RolePermissionsChangedEvent
import com.vertyll.veds.shared.messaging.avro.AvroPayloadDeserializer
import com.vertyll.veds.shared.messaging.kafka.ProcessedEventGuard
import com.vertyll.veds.sharedauthz.PermissionCatalogue
import com.vertyll.veds.translation.infrastructure.persistence.entity.RolePermissionsJpaEntity
import com.vertyll.veds.translation.infrastructure.persistence.repository.RolePermissionsJpaRepository
import org.slf4j.LoggerFactory
import org.springframework.data.repository.findByIdOrNull
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.messaging.handler.annotation.Header
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@Component
internal class RolePermissionsConsumerAdapter(
    private val avroPayloadDeserializer: AvroPayloadDeserializer,
    private val repository: RolePermissionsJpaRepository,
    private val catalogue: PermissionCatalogue,
    private val processedEventGuard: ProcessedEventGuard,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    private companion object {
        const val TOPIC = "role-permissions-changed"
        const val GROUP_PREFIX = "translation-service:"
        const val GLOBAL_SCOPE = "GLOBAL"
    }

    @KafkaListener(topics = [TOPIC])
    @Transactional
    fun onRolePermissionsChanged(
        @Payload payload: ByteArray,
        @Header(name = "eventId", required = false) eventId: String?,
    ) {
        if (eventId != null && !processedEventGuard.claim(eventId, GROUP_PREFIX + TOPIC)) {
            logger.debug("Already handled {} from {}", eventId, TOPIC)
            return
        }

        val event = avroPayloadDeserializer.deserialize(TOPIC, payload) as RolePermissionsChangedEvent
        val role = event.role.toString()

        if (event.scope.toString() != GLOBAL_SCOPE) {
            logger.debug("Ignoring {} role {}: the catalogue is administered platform-wide", event.scope, role)
            return
        }

        if (event.removed) {
            repository.deleteById(role)
            logger.info("Dropped the permission projection for role {}", role)
            return
        }

        val entity = repository.findByIdOrNull(role) ?: RolePermissionsJpaEntity(roleName = role)
        entity.permissions =
            event.permissions
                .mapTo(mutableSetOf()) { it.toString() }
                .intersect(catalogue.names)
                .toMutableSet()
        entity.unrestricted = event.unrestricted
        entity.updatedAt = Instant.now()
        repository.save(entity)
    }
}
