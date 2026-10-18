package com.vertyll.veds.iam.infrastructure.config

import com.vertyll.veds.iam.domain.model.Role
import com.vertyll.veds.iam.domain.model.RoleType
import com.vertyll.veds.iam.domain.repository.RoleRepository
import org.slf4j.LoggerFactory
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
@Order(1)
internal class RoleInitializer(
    private val roleRepository: RoleRepository,
) : ApplicationRunner {
    private companion object {
        private val logger = LoggerFactory.getLogger(RoleInitializer::class.java)

        private val ROLE_DESCRIPTIONS =
            mapOf(
                RoleType.USER to "iam.role.user.description",
                RoleType.ADMIN to "iam.role.admin.description",
            )
    }

    @Transactional
    override fun run(args: ApplicationArguments) {
        ROLE_DESCRIPTIONS.forEach { (roleType, description) ->
            val existing = roleRepository.findByName(roleType.value)
            if (existing != null) {
                ensureAdministratorStaysUnrestricted(roleType, existing)
                return@forEach
            }

            roleRepository.save(
                Role.create(
                    name = roleType.value,
                    description = description,
                    unrestricted = roleType == RoleType.ADMIN,
                ),
            )
            logger.info("Created default role {}", roleType.value)
        }
    }

    private fun ensureAdministratorStaysUnrestricted(
        roleType: RoleType,
        existing: Role,
    ) {
        if (roleType != RoleType.ADMIN || existing.unrestricted) return

        roleRepository.save(existing.copy(unrestricted = true))
        logger.info("Restored the administrator role's unrestricted grant")
    }
}
