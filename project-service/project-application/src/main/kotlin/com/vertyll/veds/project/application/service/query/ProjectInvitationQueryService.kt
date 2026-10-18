package com.vertyll.veds.project.application.service.query

import com.vertyll.veds.project.application.dto.ProjectInvitationResponse
import com.vertyll.veds.project.application.port.inbound.query.ProjectInvitationQueryUseCase
import com.vertyll.veds.project.domain.repository.ProjectInvitationRepository
import com.vertyll.veds.project.domain.repository.ProjectRepository

class ProjectInvitationQueryService(
    private val invitationRepository: ProjectInvitationRepository,
    private val projectRepository: ProjectRepository,
) : ProjectInvitationQueryUseCase {
    override fun getMyInvitations(actorEmail: String): List<ProjectInvitationResponse> {
        val invitations = invitationRepository.findAllByInviteeEmail(actorEmail).filter { it.isPending }
        val projects = projectRepository.findAllByIds(invitations.map { it.projectId }).associateBy { it.id }
        return invitations.map { ProjectInvitationResponse.from(it, projects[it.projectId]?.name) }
    }
}
