@file:OptIn(ExperimentalUuidApi::class)

package com.vertyll.veds.project.application

import com.vertyll.veds.project.application.dto.Actor
import com.vertyll.veds.project.domain.model.LanguageTag
import com.vertyll.veds.project.domain.model.Project
import com.vertyll.veds.project.domain.model.ProjectPermission
import com.vertyll.veds.project.domain.model.ProjectRole
import com.vertyll.veds.project.domain.model.ProjectRoleCode
import com.vertyll.veds.project.domain.model.ProjectType
import com.vertyll.veds.project.domain.model.ProjectTypeCode
import com.vertyll.veds.project.domain.model.Translation
import com.vertyll.veds.project.domain.model.UserRef
import java.util.UUID
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlin.uuid.toJavaUuid

internal val ENGLISH = LanguageTag("en")
internal val POLISH = LanguageTag("pl")

internal fun translation(
    name: String,
    language: LanguageTag = ENGLISH,
) = Translation(language = language, name = name)

internal fun actor(
    id: UUID = Uuid.generateV7().toJavaUuid(),
    email: String = "owner@example.com",
) = Actor(id = id, email = email, firstName = "Ada", lastName = "Lovelace")

internal fun project(
    name: String = "Apollo",
    ownerId: UUID = Uuid.generateV7().toJavaUuid(),
    isPublic: Boolean = false,
    typeId: UUID? = null,
    version: Long? = 0L,
    isActive: Boolean = true,
) = Project(
    name = name,
    ownerId = ownerId,
    isPublic = isPublic,
    typeId = typeId,
    version = version,
    isActive = isActive,
)

internal fun role(
    code: ProjectRoleCode = ProjectRoleCode.MANAGER,
    permissions: Set<ProjectPermission> = ProjectPermission.entries.toSet(),
    extraPermissions: Set<String> = emptySet(),
) = ProjectRole.create(
    code = code,
    permissions = permissions.mapTo(mutableSetOf()) { it.name } + extraPermissions,
    translations = setOf(translation(code.value)),
)

internal fun projectType(code: ProjectTypeCode = ProjectTypeCode.entries.first()) =
    ProjectType.create(code = code, translations = setOf(translation(code.name)))

internal fun userRef(
    userId: UUID = Uuid.generateV7().toJavaUuid(),
    email: String = "member@example.com",
) = UserRef(userId = userId, email = email, firstName = "Grace", lastName = "Hopper")
