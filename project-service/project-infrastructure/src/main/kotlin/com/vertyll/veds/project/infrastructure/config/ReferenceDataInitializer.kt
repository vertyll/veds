package com.vertyll.veds.project.infrastructure.config

import com.vertyll.veds.project.domain.model.LanguageTag
import com.vertyll.veds.project.domain.model.ProjectRole
import com.vertyll.veds.project.domain.model.ProjectRoleCode
import com.vertyll.veds.project.domain.model.ProjectType
import com.vertyll.veds.project.domain.model.ProjectTypeCode
import com.vertyll.veds.project.domain.model.Translation
import com.vertyll.veds.project.domain.repository.ProjectRoleRepository
import com.vertyll.veds.project.domain.repository.ProjectTypeRepository
import org.slf4j.LoggerFactory
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
internal class ReferenceDataInitializer(
    private val roleRepository: ProjectRoleRepository,
    private val typeRepository: ProjectTypeRepository,
) : ApplicationRunner {
    private companion object {
        private val logger = LoggerFactory.getLogger(ReferenceDataInitializer::class.java)

        private val ROLE_TRANSLATIONS =
            mapOf(
                ProjectRoleCode.MANAGER to
                    setOf(
                        Translation(LanguageTag.of("pl"), "Menedżer", "Zarządza projektem, członkami i zadaniami"),
                        Translation(LanguageTag.of("en"), "Manager", "Manages the project, its members and tasks"),
                    ),
                ProjectRoleCode.MEMBER to
                    setOf(
                        Translation(LanguageTag.of("pl"), "Członek", "Pracuje nad zadaniami w projekcie"),
                        Translation(LanguageTag.of("en"), "Member", "Works on tasks within the project"),
                    ),
                ProjectRoleCode.CLIENT to
                    setOf(
                        Translation(LanguageTag.of("pl"), "Klient", "Podgląda postęp projektu"),
                        Translation(LanguageTag.of("en"), "Client", "Follows the progress of the project"),
                    ),
            )

        private val TYPE_TRANSLATIONS =
            mapOf(
                ProjectTypeCode.TICKETS to
                    setOf(
                        Translation(LanguageTag.of("pl"), "Zgłoszenia", "Projekt oparty na zgłoszeniach"),
                        Translation(LanguageTag.of("en"), "Tickets", "Ticket-driven project"),
                    ),
                ProjectTypeCode.BACKLOG to
                    setOf(
                        Translation(LanguageTag.of("pl"), "Backlog", "Projekt oparty na backlogu zadań"),
                        Translation(LanguageTag.of("en"), "Backlog", "Backlog-driven project"),
                    ),
            )
    }

    @Transactional
    override fun run(args: ApplicationArguments) {
        seedRoles()
        seedTypes()
    }

    private fun seedRoles() {
        ProjectRoleCode.stock.forEach { code ->
            if (roleRepository.existsByCode(code)) {
                logger.debug("Project role already exists, skipping: {}", code)
                return@forEach
            }
            roleRepository.save(
                ProjectRole.create(
                    code = code,
                    permissions = emptySet(),
                    translations =
                        requireNotNull(ROLE_TRANSLATIONS[code]) { "no translations declared for role $code" },
                ),
            )
            logger.info("Created default project role: {}", code)
        }
    }

    private fun seedTypes() {
        ProjectTypeCode.entries.forEach { code ->
            if (typeRepository.existsByCode(code)) {
                logger.debug("Project type already exists, skipping: {}", code)
                return@forEach
            }
            typeRepository.save(
                ProjectType.create(
                    code = code,
                    translations =
                        requireNotNull(TYPE_TRANSLATIONS[code]) { "no translations declared for type $code" },
                ),
            )
            logger.info("Created default project type: {}", code)
        }
    }
}
