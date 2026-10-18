package com.vertyll.veds.project.application.service.command

import com.vertyll.veds.project.application.command.CreateProjectCommand
import com.vertyll.veds.project.application.command.UpdateProjectCommand
import com.vertyll.veds.project.application.dto.Actor
import com.vertyll.veds.project.application.dto.ProjectResponse
import com.vertyll.veds.project.application.port.inbound.command.ProjectCommandUseCase
import com.vertyll.veds.project.application.port.outbound.ProjectEventPublisherPort
import com.vertyll.veds.project.application.service.ProjectAuthorizationService
import com.vertyll.veds.project.domain.error.ProjectError
import com.vertyll.veds.project.domain.model.Project
import com.vertyll.veds.project.domain.model.ProjectMember
import com.vertyll.veds.project.domain.model.ProjectPermission
import com.vertyll.veds.project.domain.model.ProjectRoleCode
import com.vertyll.veds.project.domain.model.VersionGuard
import com.vertyll.veds.project.domain.repository.ProjectMemberRepository
import com.vertyll.veds.project.domain.repository.ProjectRepository
import com.vertyll.veds.project.domain.repository.ProjectRoleRepository
import com.vertyll.veds.project.domain.repository.ProjectTypeRepository
import com.vertyll.veds.project.domain.repository.UserDirectoryRepository
import com.vertyll.veds.sharederror.ApiException
import java.util.UUID

@Suppress("LongParameterList")
class ProjectCommandService(
    private val projectRepository: ProjectRepository,
    private val memberRepository: ProjectMemberRepository,
    private val roleRepository: ProjectRoleRepository,
    private val typeRepository: ProjectTypeRepository,
    private val userDirectory: UserDirectoryRepository,
    private val authorization: ProjectAuthorizationService,
    private val eventPublisher: ProjectEventPublisherPort,
) : ProjectCommandUseCase {
    override fun createProject(
        command: CreateProjectCommand,
        actor: Actor,
    ): ProjectResponse {
        command.typeId?.let {
            typeRepository.findById(it) ?: throw ApiException(ProjectError.TYPE_NOT_FOUND)
        }

        val project =
            projectRepository.save(
                Project.create(
                    name = command.name,
                    description = command.description,
                    isPublic = command.isPublic,
                    typeId = command.typeId,
                    ownerId = actor.id,
                    iconFileId = command.iconFileId,
                    hiddenWorkLogEnabled = command.hiddenWorkLogEnabled,
                ),
            )

        val managerRole =
            roleRepository.findByCode(ProjectRoleCode.MANAGER)
                ?: throw ApiException(ProjectError.ROLE_NOT_CONFIGURED)

        userDirectory.save(actor.toUserRef())

        val manager =
            memberRepository.save(
                ProjectMember.create(
                    projectId = project.id,
                    userId = actor.id,
                    roleId = managerRole.id,
                ),
            )

        eventPublisher.publishProjectCreated(
            projectId = project.id,
            name = project.name,
            ownerId = actor.id,
            hiddenWorkLogEnabled = project.hiddenWorkLogEnabled,
        )
        eventPublisher.publishMemberJoined(
            projectId = project.id,
            memberId = manager.id,
            userId = manager.userId,
            roleCode = managerRole.code.value,
        )

        return ProjectResponse.from(project, authorization.effectivePermissions(project.id, actor.id))
    }

    override fun updateProject(
        projectId: UUID,
        command: UpdateProjectCommand,
        actorId: UUID,
        version: Long?,
    ): ProjectResponse {
        val project = authorization.requirePermission(projectId, actorId, ProjectPermission.EDIT_PROJECT)

        VersionGuard.requireMatch(project.version, version) {
            ApiException(ProjectError.VERSION_MISMATCH)
        }

        command.typeId?.let {
            typeRepository.findById(it) ?: throw ApiException(ProjectError.TYPE_NOT_FOUND)
        }

        val updated =
            projectRepository.save(
                project
                    .rename(command.name)
                    .describe(command.description)
                    .changeVisibility(command.isPublic)
                    .changeType(command.typeId)
                    .changeIcon(command.iconFileId)
                    .configureHiddenWorkLog(command.hiddenWorkLogEnabled),
            )

        eventPublisher.publishProjectUpdated(
            projectId = updated.id,
            name = updated.name,
            hiddenWorkLogEnabled = updated.hiddenWorkLogEnabled,
        )

        return ProjectResponse.from(updated, authorization.effectivePermissions(updated.id, actorId))
    }

    override fun archiveProject(
        projectId: UUID,
        actorId: UUID,
        version: Long?,
    ) {
        val project = authorization.requirePermission(projectId, actorId, ProjectPermission.DELETE_PROJECT)

        VersionGuard.requireMatch(project.version, version) {
            ApiException(ProjectError.VERSION_MISMATCH)
        }

        projectRepository.save(project.archive())
        eventPublisher.publishProjectArchived(project.id)
    }
}
