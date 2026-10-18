@file:OptIn(ExperimentalUuidApi::class)

package com.vertyll.veds.project.domain.model

import java.time.Instant
import java.util.UUID
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlin.uuid.toJavaUuid

data class ProjectRole(
    val id: UUID = Uuid.generateV7().toJavaUuid(),
    val code: ProjectRoleCode,
    val permissions: Set<String> = emptySet(),
    val unrestricted: Boolean = false,
    val isActive: Boolean = true,
    val translations: Set<Translation> = emptySet(),
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now(),
    val version: Long? = null,
) {
    init {
        requireAtLeastOneTranslation(translations)
    }

    fun grants(permission: ProjectPermission): Boolean = grants(permission.name)

    fun grants(permission: String): Boolean = isActive && (unrestricted || permission in permissions)

    fun translationFor(language: LanguageTag): Translation = translations.resolveFor(language)

    fun withPermissions(newPermissions: Set<String>): ProjectRole = copy(permissions = newPermissions, updatedAt = Instant.now())

    companion object {
        fun create(
            code: ProjectRoleCode,
            permissions: Set<String>,
            translations: Set<Translation>,
            unrestricted: Boolean = false,
        ): ProjectRole =
            ProjectRole(
                code = code,
                permissions = permissions,
                unrestricted = unrestricted,
                translations = translations,
            )
    }
}
