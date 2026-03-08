package com.vertyll.veds.iam.infrastructure.config

import com.vertyll.veds.iam.domain.model.entity.Role
import com.vertyll.veds.iam.domain.repository.RoleRepository
import com.vertyll.veds.sharedinfrastructure.role.RoleType
import org.slf4j.LoggerFactory
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class RoleInitializer(
    private val roleRepository: RoleRepository,
) : ApplicationRunner {
    private companion object {
        private val logger = LoggerFactory.getLogger(RoleInitializer::class.java)

        private val DEFAULT_ROLES =
            listOf(
                RoleType.USER to "Standard application user",
                RoleType.ADMIN to "Application administrator",
            )
    }

    @Transactional
    override fun run(args: ApplicationArguments) {
        DEFAULT_ROLES.forEach { (roleType, description) ->
            if (!roleRepository.existsByName(roleType.value)) {
                roleRepository.save(Role.create(name = roleType.value, description = description))
                logger.info("Created default role: {}", roleType.value)
            } else {
                logger.debug("Role already exists, skipping: {}", roleType.value)
            }
        }
    }
}
