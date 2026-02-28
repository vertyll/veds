package com.vertyll.projecta.identity.infrastructure.config

import com.vertyll.projecta.identity.domain.model.entity.Role
import com.vertyll.projecta.identity.domain.model.enums.RoleType
import com.vertyll.projecta.identity.domain.repository.RoleRepository
import org.slf4j.LoggerFactory
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class RoleInitializer(
    private val roleRepository: RoleRepository,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    @EventListener(ApplicationReadyEvent::class)
    @Transactional
    fun initializeRoles() {
        logger.info("Initializing default roles...")

        RoleType.entries.forEach { roleType ->
            if (!roleRepository.existsByName(roleType.value)) {
                val role =
                    Role(
                        name = roleType.value,
                        description = "Default ${roleType.value} role",
                    )
                roleRepository.save(role)
                logger.info("Created role: ${roleType.value}")
            } else {
                logger.debug("Role ${roleType.value} already exists")
            }
        }

        logger.info("Role initialization completed")
    }
}
