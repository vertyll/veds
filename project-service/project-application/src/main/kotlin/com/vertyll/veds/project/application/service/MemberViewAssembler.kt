package com.vertyll.veds.project.application.service

import com.vertyll.veds.project.application.dto.ProjectMemberResponse
import com.vertyll.veds.project.application.dto.ProjectRoleResponse
import com.vertyll.veds.project.domain.error.ProjectError
import com.vertyll.veds.project.domain.model.LanguageTag
import com.vertyll.veds.project.domain.model.ProjectMember
import com.vertyll.veds.project.domain.repository.ProjectRoleRepository
import com.vertyll.veds.project.domain.repository.UserDirectoryRepository
import com.vertyll.veds.sharederror.ApiException

class MemberViewAssembler(
    private val roleRepository: ProjectRoleRepository,
    private val userDirectory: UserDirectoryRepository,
) {
    fun assemble(
        members: List<ProjectMember>,
        language: LanguageTag,
    ): List<ProjectMemberResponse> {
        if (members.isEmpty()) return emptyList()

        val users = userDirectory.findAllByIds(members.map { it.userId }).associateBy { it.userId }
        val roles = roleRepository.findAll().associateBy { it.id }

        return members.map { member ->
            val user =
                users[member.userId]
                    ?: throw ApiException(
                        ProjectError.MEMBER_NOT_FOUND,
                        mapOf("memberId" to member.id.toString(), "userId" to member.userId.toString()),
                    )
            val role =
                roles[member.roleId]
                    ?: throw ApiException(
                        ProjectError.ROLE_NOT_FOUND,
                        mapOf("roleId" to member.roleId.toString()),
                    )

            ProjectMemberResponse.from(
                member = member,
                user = user,
                role = ProjectRoleResponse.from(role, language),
            )
        }
    }
}
