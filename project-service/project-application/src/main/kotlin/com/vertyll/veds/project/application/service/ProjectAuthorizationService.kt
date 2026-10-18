package com.vertyll.veds.project.application.service

import com.vertyll.veds.project.domain.error.ProjectError
import com.vertyll.veds.project.domain.model.Project
import com.vertyll.veds.project.domain.model.ProjectPermission
import com.vertyll.veds.project.domain.repository.ProjectMemberRepository
import com.vertyll.veds.project.domain.repository.ProjectRepository
import com.vertyll.veds.project.domain.repository.ProjectRoleRepository
import com.vertyll.veds.project.domain.service.AccessDecision
import com.vertyll.veds.project.domain.service.ProjectAccessPolicy
import com.vertyll.veds.sharederror.ApiException
import java.util.UUID

class ProjectAuthorizationService(
    private val projectRepository: ProjectRepository,
    private val memberRepository: ProjectMemberRepository,
    private val roleRepository: ProjectRoleRepository,
) {
    private companion object {
        private const val PROJECT_ID_PARAM = "projectId"
    }

    fun requirePermission(
        projectId: UUID,
        actorId: UUID,
        permission: ProjectPermission,
    ): Project {
        val project =
            projectRepository.findById(projectId)
                ?: throw ApiException(ProjectError.PROJECT_NOT_FOUND)

        val member = memberRepository.findByProjectIdAndUserId(projectId, actorId)
        val role = member?.let { roleRepository.findById(it.roleId) }

        if (!ProjectAccessPolicy.canView(project, actorId, member, role)) {
            throw ApiException(ProjectError.PROJECT_NOT_FOUND)
        }

        return when (val decision = ProjectAccessPolicy.permits(project, actorId, member, role, permission)) {
            is AccessDecision.Permit -> {
                project
            }

            is AccessDecision.Deny -> {
                throw ApiException(decision.reason, mapOf(PROJECT_ID_PARAM to projectId.toString()))
            }
        }
    }

    fun effectivePermissions(
        projectId: UUID,
        actorId: UUID,
    ): Set<String> {
        val project =
            projectRepository.findById(projectId)
                ?: throw ApiException(ProjectError.PROJECT_NOT_FOUND)
        val member = memberRepository.findByProjectIdAndUserId(projectId, actorId)
        val role = member?.let { roleRepository.findById(it.roleId) }
        return ProjectAccessPolicy.permissionsOf(project, actorId, member, role)
    }
}
