package com.vertyll.veds.project.application.service.command

import com.vertyll.veds.project.application.command.CreateCategoryCommand
import com.vertyll.veds.project.application.command.UpdateCategoryCommand
import com.vertyll.veds.project.application.dto.ProjectCategoryResponse
import com.vertyll.veds.project.application.port.inbound.command.ProjectCategoryCommandUseCase
import com.vertyll.veds.project.application.port.outbound.ProjectEventPublisherPort
import com.vertyll.veds.project.application.service.ProjectAuthorizationService
import com.vertyll.veds.project.application.service.TranslationCompletenessValidator
import com.vertyll.veds.project.domain.error.ProjectError
import com.vertyll.veds.project.domain.model.LanguageTag
import com.vertyll.veds.project.domain.model.ProjectCategory
import com.vertyll.veds.project.domain.model.ProjectPermission
import com.vertyll.veds.project.domain.model.VersionGuard
import com.vertyll.veds.project.domain.repository.ProjectCategoryRepository
import com.vertyll.veds.sharederror.ApiException
import java.util.UUID

class ProjectCategoryCommandService(
    private val categoryRepository: ProjectCategoryRepository,
    private val authorization: ProjectAuthorizationService,
    private val eventPublisher: ProjectEventPublisherPort,
    private val translationCompleteness: TranslationCompletenessValidator,
) : ProjectCategoryCommandUseCase {
    override fun createCategory(
        projectId: UUID,
        command: CreateCategoryCommand,
        actorId: UUID,
        language: LanguageTag,
    ): ProjectCategoryResponse {
        authorization.requirePermission(projectId, actorId, ProjectPermission.EDIT_PROJECT)
        translationCompleteness.validate(command.translations)

        val category =
            categoryRepository.save(
                ProjectCategory.create(
                    projectId = projectId,
                    color = command.color,
                    translations = command.translations,
                ),
            )

        eventPublisher.publishCategoryChanged(
            projectId = projectId,
            categoryId = category.id,
            names = category.translations.associate { it.language.value to it.name },
            color = category.color,
            removed = false,
        )

        return ProjectCategoryResponse.from(category, language)
    }

    override fun updateCategory(
        projectId: UUID,
        categoryId: UUID,
        command: UpdateCategoryCommand,
        actorId: UUID,
        language: LanguageTag,
        version: Long?,
    ): ProjectCategoryResponse {
        authorization.requirePermission(projectId, actorId, ProjectPermission.EDIT_PROJECT)
        translationCompleteness.validate(command.translations)

        val category = loadOwnedCategory(projectId, categoryId)

        VersionGuard.requireMatch(category.version, version) {
            ApiException(ProjectError.VERSION_MISMATCH)
        }

        val updated =
            categoryRepository.save(
                category
                    .recolor(command.color)
                    .retranslate(command.translations)
                    .let { if (command.isActive) it.activate() else it.deactivate() },
            )

        eventPublisher.publishCategoryChanged(
            projectId = projectId,
            categoryId = updated.id,
            names = updated.translations.associate { it.language.value to it.name },
            color = updated.color,
            removed = !updated.isActive,
        )

        return ProjectCategoryResponse.from(updated, language)
    }

    override fun deleteCategory(
        projectId: UUID,
        categoryId: UUID,
        actorId: UUID,
    ) {
        authorization.requirePermission(projectId, actorId, ProjectPermission.EDIT_PROJECT)

        val category = loadOwnedCategory(projectId, categoryId)
        categoryRepository.delete(category.id)

        eventPublisher.publishCategoryChanged(
            projectId = projectId,
            categoryId = category.id,
            names = category.translations.associate { it.language.value to it.name },
            color = category.color,
            removed = true,
        )
    }

    private fun loadOwnedCategory(
        projectId: UUID,
        categoryId: UUID,
    ): ProjectCategory {
        val category =
            categoryRepository.findById(categoryId)
                ?: throw ApiException(ProjectError.CATEGORY_NOT_FOUND)
        if (category.projectId != projectId) {
            throw ApiException(ProjectError.CATEGORY_NOT_FOUND)
        }
        return category
    }
}
