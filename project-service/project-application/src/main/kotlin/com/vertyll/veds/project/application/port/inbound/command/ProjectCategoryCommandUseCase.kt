package com.vertyll.veds.project.application.port.inbound.command

import com.vertyll.veds.project.application.command.CreateCategoryCommand
import com.vertyll.veds.project.application.command.UpdateCategoryCommand
import com.vertyll.veds.project.application.dto.ProjectCategoryResponse
import com.vertyll.veds.project.domain.model.LanguageTag
import java.util.UUID

interface ProjectCategoryCommandUseCase {
    fun createCategory(
        projectId: UUID,
        command: CreateCategoryCommand,
        actorId: UUID,
        language: LanguageTag,
    ): ProjectCategoryResponse

    fun updateCategory(
        projectId: UUID,
        categoryId: UUID,
        command: UpdateCategoryCommand,
        actorId: UUID,
        language: LanguageTag,
        version: Long? = null,
    ): ProjectCategoryResponse

    fun deleteCategory(
        projectId: UUID,
        categoryId: UUID,
        actorId: UUID,
    )
}
