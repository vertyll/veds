package com.vertyll.veds.project.application.service.command

import com.vertyll.veds.project.application.command.CreateStatusCommand
import com.vertyll.veds.project.application.command.UpdateStatusCommand
import com.vertyll.veds.project.application.dto.ProjectStatusResponse
import com.vertyll.veds.project.application.port.inbound.command.ProjectStatusCommandUseCase
import com.vertyll.veds.project.application.port.outbound.ProjectEventPublisherPort
import com.vertyll.veds.project.application.service.ProjectAuthorizationService
import com.vertyll.veds.project.application.service.TranslationCompletenessValidator
import com.vertyll.veds.project.domain.error.ProjectError
import com.vertyll.veds.project.domain.model.LanguageTag
import com.vertyll.veds.project.domain.model.ProjectPermission
import com.vertyll.veds.project.domain.model.ProjectStatus
import com.vertyll.veds.project.domain.model.VersionGuard
import com.vertyll.veds.project.domain.repository.ProjectStatusRepository
import com.vertyll.veds.sharederror.ApiException
import java.util.UUID

class ProjectStatusCommandService(
    private val statusRepository: ProjectStatusRepository,
    private val authorization: ProjectAuthorizationService,
    private val eventPublisher: ProjectEventPublisherPort,
    private val translationCompleteness: TranslationCompletenessValidator,
) : ProjectStatusCommandUseCase {
    override fun createStatus(
        projectId: UUID,
        command: CreateStatusCommand,
        actorId: UUID,
        language: LanguageTag,
    ): ProjectStatusResponse {
        authorization.requirePermission(projectId, actorId, ProjectPermission.EDIT_PROJECT)
        translationCompleteness.validate(command.translations)

        val status =
            statusRepository.save(
                ProjectStatus.create(
                    projectId = projectId,
                    color = command.color,
                    translations = command.translations,
                ),
            )

        eventPublisher.publishStatusChanged(
            projectId = projectId,
            statusId = status.id,
            names = status.translations.associate { it.language.value to it.name },
            color = status.color,
            removed = false,
        )

        return ProjectStatusResponse.from(status, language)
    }

    override fun updateStatus(
        projectId: UUID,
        statusId: UUID,
        command: UpdateStatusCommand,
        actorId: UUID,
        language: LanguageTag,
        version: Long?,
    ): ProjectStatusResponse {
        authorization.requirePermission(projectId, actorId, ProjectPermission.EDIT_PROJECT)
        translationCompleteness.validate(command.translations)

        val status = loadOwnedStatus(projectId, statusId)

        VersionGuard.requireMatch(status.version, version) {
            ApiException(ProjectError.VERSION_MISMATCH)
        }

        val updated =
            statusRepository.save(
                status
                    .recolor(command.color)
                    .retranslate(command.translations)
                    .let { if (command.isActive) it.activate() else it.deactivate() },
            )

        eventPublisher.publishStatusChanged(
            projectId = projectId,
            statusId = updated.id,
            names = updated.translations.associate { it.language.value to it.name },
            color = updated.color,
            removed = !updated.isActive,
        )

        return ProjectStatusResponse.from(updated, language)
    }

    override fun deleteStatus(
        projectId: UUID,
        statusId: UUID,
        actorId: UUID,
    ) {
        authorization.requirePermission(projectId, actorId, ProjectPermission.EDIT_PROJECT)

        val status = loadOwnedStatus(projectId, statusId)
        statusRepository.delete(status.id)

        eventPublisher.publishStatusChanged(
            projectId = projectId,
            statusId = status.id,
            names = status.translations.associate { it.language.value to it.name },
            color = status.color,
            removed = true,
        )
    }

    private fun loadOwnedStatus(
        projectId: UUID,
        statusId: UUID,
    ): ProjectStatus {
        val status =
            statusRepository.findById(statusId)
                ?: throw ApiException(ProjectError.STATUS_NOT_FOUND)
        if (status.projectId != projectId) {
            throw ApiException(ProjectError.STATUS_NOT_FOUND)
        }
        return status
    }
}
