package com.vertyll.veds.project.application.dto

import com.vertyll.veds.project.domain.model.ProjectMember
import com.vertyll.veds.project.domain.model.UserRef
import java.time.Instant
import java.util.UUID

data class ProjectMemberResponse(
    val id: UUID,
    val projectId: UUID,
    val userId: UUID,
    val email: String,
    val displayName: String,
    val avatarFileId: UUID?,
    val roleId: UUID,
    val roleCode: String,
    val rolePermissions: Set<String>,
    val roleName: String,
    val assignedAt: Instant,
    val version: Long?,
) {
    companion object {
        fun from(
            member: ProjectMember,
            user: UserRef,
            role: ProjectRoleResponse,
        ): ProjectMemberResponse =
            ProjectMemberResponse(
                id = member.id,
                projectId = member.projectId,
                userId = member.userId,
                email = user.email,
                displayName = user.displayName,
                avatarFileId = user.avatarFileId,
                roleId = member.roleId,
                roleCode = role.code,
                rolePermissions = role.permissions,
                roleName = role.name,
                assignedAt = member.assignedAt,
                version = member.version,
            )
    }
}
