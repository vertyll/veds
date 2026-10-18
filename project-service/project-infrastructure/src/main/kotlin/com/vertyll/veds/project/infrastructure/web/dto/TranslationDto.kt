package com.vertyll.veds.project.infrastructure.web.dto

import com.vertyll.veds.project.domain.model.LanguageTag
import com.vertyll.veds.project.domain.model.Translation
import jakarta.validation.constraints.NotBlank

data class TranslationDto(
    val language: String,
    @field:NotBlank(message = "validation.project.translation_name_required")
    val name: String,
    val description: String? = null,
) {
    fun toDomain(): Translation =
        Translation(
            language = LanguageTag.of(language),
            name = name,
            description = description,
        )

    companion object {
        fun from(translation: Translation): TranslationDto =
            TranslationDto(
                language = translation.language.value,
                name = translation.name,
                description = translation.description,
            )
    }
}
