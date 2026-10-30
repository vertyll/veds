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

    private companion object {
        private val logger = LoggerFactory.getLogger(ReferenceDataInitializer::class.java)

        private val ROLE_TRANSLATIONS =
            mapOf(
                ProjectRoleCode.MANAGER to
                    setOf(
                        Translation(
                            language = LanguageTag.of("pl"),
                            name = "Menedżer",
                            description = "Zarządza projektem, członkami i zadaniami",
                        ),
                        Translation(
                            language = LanguageTag.of("en"),
                            name = "Manager",
                            description = "Manages the project, its members and tasks",
                        ),
                    ),
                ProjectRoleCode.MEMBER to
                    setOf(
                        Translation(language = LanguageTag.of("pl"), name = "Członek", description = "Pracuje nad zadaniami w projekcie"),
                        Translation(language = LanguageTag.of("en"), name = "Member", description = "Works on tasks within the project"),
                    ),
                ProjectRoleCode.CLIENT to
                    setOf(
                        Translation(language = LanguageTag.of("pl"), name = "Klient", description = "Podgląda postęp projektu"),
                        Translation(language = LanguageTag.of("en"), name = "Client", description = "Follows the progress of the project"),
                    ),
            )

        private val TYPE_TRANSLATIONS =
            mapOf(
                ProjectTypeCode.TICKETS to
                    setOf(
                        Translation(language = LanguageTag.of("pl"), name = "Zgłoszenia", description = "Projekt oparty na zgłoszeniach"),
                        Translation(language = LanguageTag.of("en"), name = "Tickets", description = "Ticket-driven project"),
                    ),
                ProjectTypeCode.BACKLOG to
                    setOf(
                        Translation(language = LanguageTag.of("pl"), name = "Backlog", description = "Projekt oparty na backlogu zadań"),
                        Translation(language = LanguageTag.of("en"), name = "Backlog", description = "Backlog-driven project"),
                    ),
            )
    }
}
