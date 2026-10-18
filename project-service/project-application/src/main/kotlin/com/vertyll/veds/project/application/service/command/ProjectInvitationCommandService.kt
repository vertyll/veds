package com.vertyll.veds.project.application.service.command

import com.vertyll.veds.project.application.command.InviteMemberCommand
import com.vertyll.veds.project.application.dto.Actor
import com.vertyll.veds.project.application.dto.ProjectInvitationResponse
import com.vertyll.veds.project.application.port.inbound.command.ProjectInvitationCommandUseCase
import com.vertyll.veds.project.application.port.outbound.ProjectEventPublisherPort
import com.vertyll.veds.project.application.port.outbound.UseCaseLogger
import com.vertyll.veds.project.application.saga.model.SagaStepNames
import com.vertyll.veds.project.application.saga.model.SagaTypes
import com.vertyll.veds.project.application.service.ProjectAuthorizationService
import com.vertyll.veds.project.domain.error.ProjectError
import com.vertyll.veds.project.domain.model.ProjectInvitation
import com.vertyll.veds.project.domain.model.ProjectMember
import com.vertyll.veds.project.domain.model.ProjectPermission
import com.vertyll.veds.project.domain.model.ProjectRoleCode
import com.vertyll.veds.project.domain.repository.ProjectInvitationRepository
import com.vertyll.veds.project.domain.repository.ProjectMemberRepository
import com.vertyll.veds.project.domain.repository.ProjectRepository
import com.vertyll.veds.project.domain.repository.ProjectRoleRepository
import com.vertyll.veds.project.domain.repository.UserDirectoryRepository
import com.vertyll.veds.shared.saga.SagaProcessPort
import com.vertyll.veds.shared.saga.SagaStepStatus
import com.vertyll.veds.sharederror.ApiException
import java.time.Instant
import java.util.UUID

@Suppress("LongParameterList")
class ProjectInvitationCommandService(
    private val invitationRepository: ProjectInvitationRepository,
    private val memberRepository: ProjectMemberRepository,
    private val roleRepository: ProjectRoleRepository,
    private val projectRepository: ProjectRepository,
    private val userDirectory: UserDirectoryRepository,
    private val authorization: ProjectAuthorizationService,
    private val eventPublisher: ProjectEventPublisherPort,
    private val sagaProcess: SagaProcessPort,
    private val logger: UseCaseLogger,
) : ProjectInvitationCommandUseCase {
    override fun invite(
        projectId: UUID,
        command: InviteMemberCommand,
        actorId: UUID,
    ): ProjectInvitationResponse {
        val project = authorization.requirePermission(projectId, actorId, ProjectPermission.INVITE_USERS)

        invitationRepository.findPendingByProjectIdAndEmail(projectId, command.email)?.let {
            throw ApiException(ProjectError.INVITATION_ALREADY_SENT)
        }

        val role =
            command.roleId
                ?.let { roleRepository.findById(it) ?: throw ApiException(ProjectError.ROLE_NOT_FOUND) }
                ?: roleRepository.findByCode(ProjectRoleCode.MEMBER)
                ?: throw ApiException(ProjectError.ROLE_NOT_CONFIGURED)

        val sagaId =
            sagaProcess
                .startSaga(
                    sagaType = SagaTypes.PROJECT_INVITATION,
                    payload = mapOf("projectId" to projectId.toString(), "email" to command.email),
                ).id

        return try {
            val invitation =
                invitationRepository.save(
                    ProjectInvitation.create(
                        projectId = projectId,
                        inviteeEmail = command.email,
                        inviterId = actorId,
                        roleId = role.id,
                    ),
                )

            sagaProcess.recordSagaStep(
                sagaId = sagaId,
                stepName = SagaStepNames.PERSIST_INVITATION,
                status = SagaStepStatus.COMPLETED,
                payload = mapOf("invitationId" to invitation.id.toString()),
            )

            eventPublisher.publishMemberInvited(
                projectId = projectId,
                projectName = project.name,
                invitationId = invitation.id,
                inviteeEmail = invitation.inviteeEmail,
                inviterId = actorId,
                sagaId = sagaId,
            )

            sagaProcess.recordSagaStep(
                sagaId = sagaId,
                stepName = SagaStepNames.REQUEST_INVITATION_MAIL,
                status = SagaStepStatus.COMPLETED,
                payload = mapOf("invitationId" to invitation.id.toString()),
            )

            sagaProcess.markAwaitingResponse(sagaId)

            ProjectInvitationResponse.from(invitation, project.name)
        } catch (e: ApiException) {
            failSaga(sagaId, e.error.key)
            throw e
        }
    }

    override fun acceptInvitation(
        invitationId: UUID,
        actor: Actor,
    ): ProjectInvitationResponse {
        val invitation = loadPendingInvitationFor(invitationId, actor.email)

        if (memberRepository.findByProjectIdAndUserId(invitation.projectId, actor.id) != null) {
            throw ApiException(ProjectError.MEMBER_ALREADY_JOINED)
        }

        val accepted = invitationRepository.save(invitation.accept(actor.id))

        userDirectory.save(actor.toUserRef())

        val member =
            memberRepository.save(
                ProjectMember.create(
                    projectId = invitation.projectId,
                    userId = actor.id,
                    roleId = invitation.roleId,
                ),
            )

        val role =
            roleRepository.findById(invitation.roleId)
                ?: throw ApiException(
                    ProjectError.ROLE_NOT_FOUND,
                    mapOf("roleId" to invitation.roleId.toString()),
                )

        eventPublisher.publishMemberJoined(
            projectId = invitation.projectId,
            memberId = member.id,
            userId = actor.id,
            roleCode = role.code.value,
        )

        val projectName = projectRepository.findById(invitation.projectId)?.name
        return ProjectInvitationResponse.from(accepted, projectName)
    }

    override fun rejectInvitation(
        invitationId: UUID,
        actor: Actor,
    ): ProjectInvitationResponse {
        val invitation = loadPendingInvitationFor(invitationId, actor.email)
        val rejected = invitationRepository.save(invitation.reject(actor.id))
        val projectName = projectRepository.findById(invitation.projectId)?.name
        return ProjectInvitationResponse.from(rejected, projectName)
    }

    override fun expireOverdueInvitations(): Int {
        val now = Instant.now()
        val overdue = invitationRepository.findAllPendingExpiredBefore(now)
        overdue.forEach { invitationRepository.save(it.expire()) }
        if (overdue.isNotEmpty()) {
            logger.info("Expired {} overdue project invitations", overdue.size)
        }
        return overdue.size
    }

    private fun loadPendingInvitationFor(
        invitationId: UUID,
        actorEmail: String,
    ): ProjectInvitation {
        val invitation =
            invitationRepository.findById(invitationId)
                ?: throw ApiException(ProjectError.INVITATION_NOT_FOUND)

        if (!invitation.inviteeEmail.equals(actorEmail, ignoreCase = true)) {
            throw ApiException(ProjectError.INVITATION_NOT_ADDRESSED_TO_CALLER)
        }
        if (!invitation.isPending) {
            throw ApiException(ProjectError.INVITATION_NOT_PENDING)
        }
        if (invitation.hasExpiredAt(Instant.now())) {
            invitationRepository.save(invitation.expire())
            throw ApiException(ProjectError.INVITATION_EXPIRED)
        }
        return invitation
    }

    private fun failSaga(
        sagaId: String,
        message: String,
    ) {
        logger.error("Project invitation saga {} failed: {}", sagaId, message)
        sagaProcess.recordSagaStep(
            sagaId = sagaId,
            stepName = SagaStepNames.PERSIST_INVITATION,
            status = SagaStepStatus.FAILED,
            payload = mapOf("error" to message),
        )
        sagaProcess.markSagaFailed(sagaId, message)
    }
}
