package com.vertyll.veds.project.domain.service

import com.vertyll.veds.project.domain.model.Project
import com.vertyll.veds.project.domain.model.ProjectMember
import com.vertyll.veds.project.domain.model.ProjectPermission
import com.vertyll.veds.project.domain.model.ProjectRole
import java.util.UUID

data class AccessRequest(
    val subjectId: UUID,
    val action: ProjectPermission,
    val project: Project,
    val membership: ProjectMember?,
    val role: ProjectRole?,
) {
    val hasCoherentMembership: Boolean
        get() = membership != null && role != null && membership.userId == subjectId && membership.roleId == role.id

    val isOwner: Boolean
        get() = project.isOwnedBy(subjectId)

    val isMutating: Boolean
        get() =
            action in
                setOf(
                    ProjectPermission.EDIT_PROJECT,
                    ProjectPermission.DELETE_PROJECT,
                    ProjectPermission.INVITE_USERS,
                    ProjectPermission.MANAGE_MEMBERS,
                )
}
