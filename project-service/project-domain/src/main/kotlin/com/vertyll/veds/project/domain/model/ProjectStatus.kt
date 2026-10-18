@file:OptIn(ExperimentalUuidApi::class)

package com.vertyll.veds.project.domain.model

import java.time.Instant
import java.util.UUID
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlin.uuid.toJavaUuid

data class ProjectStatus(
    val id: UUID = Uuid.generateV7().toJavaUuid(),
    val projectId: UUID,
    val color: String,
    val isActive: Boolean = true,
    val translations: Set<Translation> = emptySet(),
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now(),
    val version: Long? = null,
) {
    init {
        require(color.isNotBlank()) { "status color must not be blank" }
        requireAtLeastOneTranslation(translations)
    }

    fun translationFor(language: LanguageTag): Translation = translations.resolveFor(language)

    fun recolor(newColor: String): ProjectStatus = copy(color = newColor, updatedAt = Instant.now())

    fun retranslate(newTranslations: Set<Translation>): ProjectStatus = copy(translations = newTranslations, updatedAt = Instant.now())

    fun deactivate(): ProjectStatus = copy(isActive = false, updatedAt = Instant.now())

    fun activate(): ProjectStatus = copy(isActive = true, updatedAt = Instant.now())

    companion object {
        fun create(
            projectId: UUID,
            color: String,
            translations: Set<Translation>,
        ): ProjectStatus =
            ProjectStatus(
                projectId = projectId,
                color = color,
                translations = translations,
            )
    }
}
