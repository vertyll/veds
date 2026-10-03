package com.vertyll.veds.project.application.service

import com.vertyll.veds.project.application.port.inbound.ProjectCompensationUseCase
import com.vertyll.veds.project.application.port.outbound.UseCaseLogger
import com.vertyll.veds.project.application.saga.model.ProjectCompensationCommand
import com.vertyll.veds.project.domain.repository.ProjectInvitationRepository
import com.vertyll.veds.project.domain.repository.ProjectRepository
import java.util.UUID

class ProjectCompensationService(
    private val invitationRepository: ProjectInvitationRepository,
    private val projectRepository: ProjectRepository,
    private val logger: UseCaseLogger,
) : ProjectCompensationUseCase {
    override fun compensate(command: ProjectCompensationCommand) {
        when (command) {
            is ProjectCompensationCommand.RevokeInvitation -> {
                revokeInvitation(command.invitationId, command.reason)
            }

            is ProjectCompensationCommand.RestoreProject -> {
                restoreProject(command.projectId, command.reason)
            }
        }
    }

    private fun revokeInvitation(
        invitationId: String,
        reason: String,
    ) {
        logger.info("Compensating PersistInvitation - revoking invitation {} ({})", invitationId, reason)
        val invitation =
            invitationRepository.findById(UUID.fromString(invitationId))
                ?: run {
                    logger.warn("Nothing to compensate: invitation {} no longer exists", invitationId)
                    return
                }
        if (invitation.isPending) {
            invitationRepository.save(invitation.expire())
        }
    }

    private fun restoreProject(
        projectId: String,
        reason: String,
    ) {
        logger.info("Compensating ArchiveProject - restoring project {} ({})", projectId, reason)
        val project =
            projectRepository.findById(UUID.fromString(projectId))
                ?: run {
                    logger.warn("Nothing to compensate: project {} no longer exists", projectId)
                    return
                }
        if (!project.isActive) {
            projectRepository.save(project.restore())
        }
    }
}
