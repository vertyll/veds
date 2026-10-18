package com.vertyll.veds.project.infrastructure.persistence.entity

import com.vertyll.veds.project.domain.model.LanguageTag
import com.vertyll.veds.project.domain.model.Translation
import jakarta.persistence.Column
import jakarta.persistence.Embeddable

@Embeddable
internal data class TranslationEmbeddable(
    @Column(name = "language", nullable = false, length = 16)
    var language: String = "",
    @Column(name = "name", nullable = false)
    var name: String = "",
    @Column(name = "description", columnDefinition = "TEXT")
    var description: String? = null,
) {
    fun toDomain(): Translation =
        Translation(
            language = LanguageTag.of(language),
            name = name,
            description = description,
        )

    companion object {
        fun from(translation: Translation): TranslationEmbeddable =
            TranslationEmbeddable(
                language = translation.language.value,
                name = translation.name,
                description = translation.description,
            )
    }
}
