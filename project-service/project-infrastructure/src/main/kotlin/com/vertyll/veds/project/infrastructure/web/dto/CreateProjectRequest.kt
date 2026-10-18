package com.vertyll.veds.project.infrastructure.web.dto

import com.vertyll.veds.project.application.command.CreateProjectCommand
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.util.UUID

data class CreateProjectRequest(
    @field:NotBlank(message = "validation.project.name_required")
    @field:Size(max = 255, message = "validation.project.name_too_long")
    val name: String = "",
    @field:Size(max = 5000, message = "validation.project.description_too_long")
    val description: String? = null,
    val isPublic: Boolean = false,
    val typeId: UUID? = null,
    val iconFileId: UUID? = null,
    val hiddenWorkLogEnabled: Boolean = false,
) {
    fun toCommand(): CreateProjectCommand =
        CreateProjectCommand(
            name = name,
            description = description,
            isPublic = isPublic,
            typeId = typeId,
            iconFileId = iconFileId,
            hiddenWorkLogEnabled = hiddenWorkLogEnabled,
        )
}
