package com.vertyll.veds.project.infrastructure.web.dto

import com.vertyll.veds.project.application.command.CreateStatusCommand
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty

data class CreateProjectStatusRequest(
    @field:NotBlank(message = "validation.project.color_required")
    val color: String = "",
    @field:NotEmpty(message = "validation.project.translations_required")
    val translations: List<@Valid TranslationDto> = emptyList(),
) {
    fun toCommand(): CreateStatusCommand =
        CreateStatusCommand(
            color = color,
            translations = translations.map { it.toDomain() }.toSet(),
        )
}
