package com.vertyll.veds.project.infrastructure.kafka.consumer

import com.vertyll.veds.iam.RolePermissionsChangedEvent
import com.vertyll.veds.project.domain.model.LanguageTag
import com.vertyll.veds.project.domain.model.ProjectRole
import com.vertyll.veds.project.domain.model.ProjectRoleCode
import com.vertyll.veds.project.domain.model.Translation
import com.vertyll.veds.project.domain.repository.ProjectRoleRepository
import com.vertyll.veds.project.infrastructure.kafka.ProjectKafkaTopics
import com.vertyll.veds.shared.messaging.avro.AvroPayloadDeserializer
import com.vertyll.veds.shared.messaging.kafka.ProcessedEventGuard
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.messaging.handler.annotation.Header
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
internal class RolePermissionsConsumerAdapter(
    private val avroPayloadDeserializer: AvroPayloadDeserializer,
    private val roleRepository: ProjectRoleRepository,
    private val processedEventGuard: ProcessedEventGuard,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    private companion object {
        const val GROUP_PREFIX = "project-service:"
        const val PROJECT_SCOPE = "PROJECT"
    }

    @KafkaListener(topics = [ProjectKafkaTopics.Consumed.ROLE_PERMISSIONS_CHANGED])
    @Transactional
    fun onRolePermissionsChanged(
        @Payload payload: ByteArray,
        @Header(name = "eventId", required = false) eventId: String?,
    ) {
        val topic = ProjectKafkaTopics.Consumed.ROLE_PERMISSIONS_CHANGED
        if (eventId != null && !processedEventGuard.claim(eventId, GROUP_PREFIX + topic)) {
            logger.debug("Already handled {} from {}", eventId, topic)
            return
        }

        val event = avroPayloadDeserializer.deserialize(topic, payload) as RolePermissionsChangedEvent

        if (event.scope.toString() != PROJECT_SCOPE) {
            logger.debug("Ignoring {} role {}: only a project role can be held in a project", event.scope, event.role)
            return
        }

        val code = ProjectRoleCode(event.role.toString())
        val existing = roleRepository.findByCode(code)

        if (event.removed) {
            existing?.let { roleRepository.save(it.copy(isActive = false, permissions = emptySet())) }
            logger.info("Deactivated project role {}: iam no longer defines it", code)
            return
        }

        val granted = event.permissions.mapTo(mutableSetOf()) { it.toString() }.toSet()

        roleRepository.save(
            existing
                ?.withPermissions(granted)
                ?.copy(unrestricted = event.unrestricted)
                ?: ProjectRole.create(
                    code = code,
                    permissions = granted,
                    translations = setOf(defaultTranslation(code)),
                    unrestricted = event.unrestricted,
                ),
        )
    }

    private fun defaultTranslation(code: ProjectRoleCode) = Translation(language = LanguageTag.of("en"), name = code.value)
}
