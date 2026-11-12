package com.vertyll.veds.project.application.dto

import com.vertyll.veds.project.domain.model.Translation

data class TranslationResponse(
    val language: String,
    val name: String,
    val description: String?,
) {
    companion object {
        fun from(translation: Translation): TranslationResponse =
            TranslationResponse(
                language = translation.language.value,
                name = translation.name,
                description = translation.description,
            )
    }
}
