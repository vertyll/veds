package com.vertyll.veds.project.application.service.command

import com.vertyll.veds.project.application.command.UpdateMemberRoleCommand
import com.vertyll.veds.project.application.dto.ProjectMemberResponse
import com.vertyll.veds.project.application.port.inbound.command.ProjectMembershipCommandUseCase
import com.vertyll.veds.project.application.port.outbound.ProjectEventPublisherPort
import com.vertyll.veds.project.application.service.MemberViewAssembler
import com.vertyll.veds.project.application.service.ProjectAuthorizationService
import com.vertyll.veds.project.domain.error.ProjectError
import com.vertyll.veds.project.domain.model.LanguageTag
import com.vertyll.veds.project.domain.model.ProjectMember
import com.vertyll.veds.project.domain.model.ProjectPermission
import com.vertyll.veds.project.domain.model.VersionGuard
import com.vertyll.veds.project.domain.repository.ProjectMemberRepository
import com.vertyll.veds.project.domain.repository.ProjectRoleRepository
import com.vertyll.veds.sharederror.ApiException
import java.util.UUID

class ProjectMembershipCommandService(
    private val memberRepository: ProjectMemberRepository,
    private val roleRepository: ProjectRoleRepository,
    private val memberViewAssembler: MemberViewAssembler,
    private val authorization: ProjectAuthorizationService,
    private val eventPublisher: ProjectEventPublisherPort,
) : ProjectMembershipCommandUseCase {
    override fun updateMemberRole(
        projectId: UUID,
        memberId: UUID,
        command: UpdateMemberRoleCommand,
        actorId: UUID,
        language: LanguageTag,
        version: Long?,
    ): ProjectMemberResponse {
        val project = authorization.requirePermission(projectId, actorId, ProjectPermission.MANAGE_MEMBERS)

        val member = loadOwnedMember(projectId, memberId)

        if (project.isOwnedBy(member.userId)) {
            throw ApiException(ProjectError.MEMBER_OWNER_IMMUTABLE)
        }

        VersionGuard.requireMatch(member.version, version) {
            ApiException(ProjectError.VERSION_MISMATCH)
        }

        val role =
            roleRepository.findById(command.roleId)
                ?: throw ApiException(ProjectError.ROLE_NOT_FOUND)

        val updated = memberRepository.save(member.reassignTo(role.id))

        eventPublisher.publishMemberJoined(
            projectId = projectId,
            memberId = updated.id,
            userId = updated.userId,
            roleCode = role.code.value,
        )

        return memberViewAssembler.assemble(listOf(updated), language).single()
    }

    override fun removeMember(
        projectId: UUID,
        memberId: UUID,
        actorId: UUID,
    ) {
        val project = authorization.requirePermission(projectId, actorId, ProjectPermission.MANAGE_MEMBERS)

        val member = loadOwnedMember(projectId, memberId)

        if (project.isOwnedBy(member.userId)) {
            throw ApiException(ProjectError.MEMBER_OWNER_IMMUTABLE)
        }

        memberRepository.delete(member.id)
        eventPublisher.publishMemberRemoved(projectId, member.userId)
    }

    private fun loadOwnedMember(
        projectId: UUID,
        memberId: UUID,
    ): ProjectMember {
        val member =
            memberRepository.findById(memberId)
                ?: throw ApiException(ProjectError.MEMBER_NOT_FOUND)
        if (member.projectId != projectId) {
            throw ApiException(ProjectError.MEMBER_NOT_FOUND)
        }
        return member
    }
}
