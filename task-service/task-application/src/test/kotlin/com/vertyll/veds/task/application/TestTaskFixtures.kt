@file:OptIn(ExperimentalUuidApi::class)

package com.vertyll.veds.task.application

import com.vertyll.veds.task.domain.model.ProjectCategoryRef
import com.vertyll.veds.task.domain.model.ProjectMembershipRef
import com.vertyll.veds.task.domain.model.ProjectRef
import com.vertyll.veds.task.domain.model.ProjectStatusRef
import com.vertyll.veds.task.domain.model.Task
import java.util.UUID
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlin.uuid.toJavaUuid

internal fun projectRef(
    projectId: UUID = Uuid.generateV7().toJavaUuid(),
    name: String = "Apollo",
    isActive: Boolean = true,
    hiddenWorkLogEnabled: Boolean = false,
) = ProjectRef(
    projectId = projectId,
    name = name,
    isActive = isActive,
    hiddenWorkLogEnabled = hiddenWorkLogEnabled,
)

internal fun membership(
    projectId: UUID,
    userId: UUID = Uuid.generateV7().toJavaUuid(),
    roleCode: String = "MEMBER",
) = ProjectMembershipRef(projectId = projectId, userId = userId, roleCode = roleCode)

internal fun categoryRef(
    projectId: UUID,
    categoryId: UUID = Uuid.generateV7().toJavaUuid(),
    name: String = "Bug",
) = ProjectCategoryRef(categoryId = categoryId, projectId = projectId, names = mapOf("en" to name), color = "#ff0000")

internal fun statusRef(
    projectId: UUID,
    statusId: UUID = Uuid.generateV7().toJavaUuid(),
    name: String = "In progress",
) = ProjectStatusRef(statusId = statusId, projectId = projectId, names = mapOf("en" to name), color = "#00ff00")

internal fun task(
    projectId: UUID,
    number: Int = 1,
    createdBy: UUID = Uuid.generateV7().toJavaUuid(),
    name: String = "Fix the thing",
    statusId: UUID? = null,
    categoryIds: Set<UUID> = emptySet(),
    assigneeIds: Set<UUID> = emptySet(),
    attachmentIds: Set<UUID> = emptySet(),
    accessRoleId: UUID? = null,
    version: Long? = 0L,
) = Task(
    projectId = projectId,
    number = number,
    name = name,
    statusId = statusId,
    categoryIds = categoryIds,
    assigneeIds = assigneeIds,
    attachmentIds = attachmentIds,
    accessRoleId = accessRoleId,
    createdBy = createdBy,
    version = version,
)
