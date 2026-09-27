@file:OptIn(ExperimentalUuidApi::class)

package com.vertyll.veds.project.application

import com.vertyll.veds.project.application.dto.Actor
import com.vertyll.veds.project.application.port.outbound.UseCaseLogger
import com.vertyll.veds.project.domain.model.InvitationStatus
import com.vertyll.veds.project.domain.model.LanguageTag
import com.vertyll.veds.project.domain.model.PageRequest
import com.vertyll.veds.project.domain.model.PageResult
import com.vertyll.veds.project.domain.model.Project
import com.vertyll.veds.project.domain.model.ProjectCategory
import com.vertyll.veds.project.domain.model.ProjectInvitation
import com.vertyll.veds.project.domain.model.ProjectMember
import com.vertyll.veds.project.domain.model.ProjectPermission
import com.vertyll.veds.project.domain.model.ProjectRole
import com.vertyll.veds.project.domain.model.ProjectRoleCode
import com.vertyll.veds.project.domain.model.ProjectSearchCriteria
import com.vertyll.veds.project.domain.model.ProjectStatus
import com.vertyll.veds.project.domain.model.ProjectType
import com.vertyll.veds.project.domain.model.ProjectTypeCode
import com.vertyll.veds.project.domain.model.Translation
import com.vertyll.veds.project.domain.model.UserRef
import com.vertyll.veds.project.domain.repository.ProjectCategoryRepository
import com.vertyll.veds.project.domain.repository.ProjectInvitationRepository
import com.vertyll.veds.project.domain.repository.ProjectMemberRepository
import com.vertyll.veds.project.domain.repository.ProjectRepository
import com.vertyll.veds.project.domain.repository.ProjectRoleRepository
import com.vertyll.veds.project.domain.repository.ProjectStatusRepository
import com.vertyll.veds.project.domain.repository.ProjectTypeRepository
import com.vertyll.veds.project.domain.repository.UserDirectoryRepository
import com.vertyll.veds.shared.saga.SagaProcessPort
import com.vertyll.veds.shared.saga.SagaSnapshot
import com.vertyll.veds.shared.saga.SagaStatus
import com.vertyll.veds.shared.saga.SagaStepStatus
import com.vertyll.veds.shared.saga.SagaTypeValue
import java.time.Instant
import java.util.UUID
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlin.uuid.toJavaUuid

internal val ENGLISH = LanguageTag("en")
internal val POLISH = LanguageTag("pl")

internal fun translation(
    name: String,
    language: LanguageTag = ENGLISH,
) = Translation(language = language, name = name)

internal fun actor(
    id: UUID = Uuid.generateV7().toJavaUuid(),
    email: String = "owner@example.com",
) = Actor(id = id, email = email, firstName = "Ada", lastName = "Lovelace")

internal fun project(
    name: String = "Apollo",
    ownerId: UUID = Uuid.generateV7().toJavaUuid(),
    isPublic: Boolean = false,
    typeId: UUID? = null,
    version: Long? = 0L,
    isActive: Boolean = true,
) = Project(
    name = name,
    ownerId = ownerId,
    isPublic = isPublic,
    typeId = typeId,
    version = version,
    isActive = isActive,
)

internal fun role(
    code: ProjectRoleCode = ProjectRoleCode.MANAGER,
    permissions: Set<ProjectPermission> = ProjectPermission.entries.toSet(),
    extraPermissions: Set<String> = emptySet(),
) = ProjectRole.create(
    code = code,
    permissions = permissions.mapTo(mutableSetOf()) { it.name } + extraPermissions,
    translations = setOf(translation(code.value)),
)

internal fun projectType(code: ProjectTypeCode = ProjectTypeCode.entries.first()) =
    ProjectType.create(code = code, translations = setOf(translation(code.name)))

internal fun userRef(
    userId: UUID = Uuid.generateV7().toJavaUuid(),
    email: String = "member@example.com",
) = UserRef(userId = userId, email = email, firstName = "Grace", lastName = "Hopper")

internal class InMemoryProjectRepository : ProjectRepository {
    val stored = linkedMapOf<UUID, Project>()

    fun given(vararg projects: Project) = projects.forEach { stored[it.id] = it }

    override fun save(project: Project) = project.also { stored[it.id] = it }

    override fun findById(id: UUID) = stored[id]

    override fun findAllByIds(ids: Collection<UUID>) = ids.mapNotNull { stored[it] }

    override fun search(
        criteria: ProjectSearchCriteria,
        pageRequest: PageRequest,
    ) = PageResult(content = stored.values.toList(), page = 0, size = stored.size, totalElements = stored.size.toLong())

    override fun existsById(id: UUID) = stored.containsKey(id)

    override fun delete(id: UUID) {
        stored.remove(id)
    }
}

internal class InMemoryMemberRepository : ProjectMemberRepository {
    val stored = mutableListOf<ProjectMember>()

    fun given(vararg members: ProjectMember) = members.forEach { stored += it }

    override fun save(member: ProjectMember) =
        member.also {
            stored.removeAll { existing -> existing.id == it.id }
            stored += it
        }

    override fun findById(id: UUID) = stored.firstOrNull { it.id == id }

    override fun findByProjectIdAndUserId(
        projectId: UUID,
        userId: UUID,
    ) = stored.firstOrNull { it.projectId == projectId && it.userId == userId }

    override fun findAllByProjectId(projectId: UUID) = stored.filter { it.projectId == projectId }

    override fun findAllByUserId(userId: UUID) = stored.filter { it.userId == userId }

    override fun countByProjectIds(projectIds: Collection<UUID>) =
        stored.filter { it.projectId in projectIds }.groupingBy { it.projectId }.eachCount()

    override fun delete(id: UUID) {
        stored.removeAll { it.id == id }
    }
}

internal class InMemoryRoleRepository : ProjectRoleRepository {
    val stored = mutableListOf<ProjectRole>()

    fun given(vararg roles: ProjectRole) = roles.forEach { stored += it }

    override fun save(role: ProjectRole) = role.also { stored += it }

    override fun findById(id: UUID) = stored.firstOrNull { it.id == id }

    override fun findByCode(code: ProjectRoleCode) = stored.firstOrNull { it.code == code }

    override fun existsByCode(code: ProjectRoleCode) = findByCode(code) != null

    override fun findAll() = stored.toList()
}

internal class InMemoryTypeRepository : ProjectTypeRepository {
    val stored = mutableListOf<ProjectType>()

    fun given(vararg types: ProjectType) = types.forEach { stored += it }

    override fun save(projectType: ProjectType) = projectType.also { stored += it }

    override fun findById(id: UUID) = stored.firstOrNull { it.id == id }

    override fun findByCode(code: ProjectTypeCode) = stored.firstOrNull { it.code == code }

    override fun existsByCode(code: ProjectTypeCode) = findByCode(code) != null

    override fun findAll() = stored.toList()
}

internal class InMemoryUserDirectory : UserDirectoryRepository {
    val stored = linkedMapOf<UUID, UserRef>()

    fun given(vararg users: UserRef) = users.forEach { stored[it.userId] = it }

    override fun save(user: UserRef) = user.also { stored[it.userId] = it }

    override fun findById(userId: UUID) = stored[userId]

    override fun findAllByIds(userIds: Collection<UUID>) = userIds.mapNotNull { stored[it] }

    override fun findByEmail(email: String) = stored.values.firstOrNull { it.email == email }
}

internal class InMemoryInvitationRepository : ProjectInvitationRepository {
    val stored = linkedMapOf<UUID, ProjectInvitation>()

    fun given(vararg invitations: ProjectInvitation) = invitations.forEach { stored[it.id] = it }

    override fun save(invitation: ProjectInvitation) = invitation.also { stored[it.id] = it }

    override fun findById(id: UUID) = stored[id]

    override fun findAllByProjectId(projectId: UUID) = stored.values.filter { it.projectId == projectId }

    override fun findAllByInviteeEmail(inviteeEmail: String) =
        stored.values.filter { it.inviteeEmail.equals(inviteeEmail, ignoreCase = true) }

    override fun findPendingByProjectIdAndEmail(
        projectId: UUID,
        inviteeEmail: String,
    ) = stored.values.firstOrNull {
        it.projectId == projectId && it.inviteeEmail.equals(inviteeEmail, ignoreCase = true) && it.isPending
    }

    override fun findAllPendingExpiredBefore(now: Instant) = stored.values.filter { it.hasExpiredAt(now) }

    override fun countByProjectIdAndStatus(
        projectId: UUID,
        status: InvitationStatus,
    ) = stored.values.count { it.projectId == projectId && it.status == status }.toLong()
}

internal class RecordingSagaProcess : SagaProcessPort {
    val trail = mutableListOf<String>()
    var started: SagaSnapshot? = null

    override fun startSaga(
        sagaType: SagaTypeValue,
        payload: Map<String, Any?>,
    ): SagaSnapshot =
        SagaSnapshot(id = "saga-1", type = sagaType.value, status = SagaStatus.STARTED, payload = payload.toString())
            .also {
                started = it
                trail += "start(${sagaType.value})"
            }

    override fun recordSagaStep(
        sagaId: String,
        stepName: SagaTypeValue,
        status: SagaStepStatus,
        payload: Map<String, Any?>,
    ) {
        trail += "step(${stepName.value},$status)"
    }

    override fun markSagaCompleted(sagaId: String) {
        trail += "completed"
    }

    override fun markSagaFailed(
        sagaId: String,
        errorMessage: String,
    ) {
        trail += "failed($errorMessage)"
    }

    override fun markAwaitingResponse(sagaId: String) {
        trail += "awaiting"
    }

    override fun findSagaById(sagaId: String) = started
}

internal object SilentLogger : UseCaseLogger {
    override fun debug(
        message: String,
        vararg args: Any?,
    ) = Unit

    override fun info(
        message: String,
        vararg args: Any?,
    ) = Unit

    override fun warn(
        message: String,
        vararg args: Any?,
    ) = Unit

    override fun error(
        message: String,
        vararg args: Any?,
    ) = Unit
}

internal class InMemoryCategoryRepository : ProjectCategoryRepository {
    val stored = linkedMapOf<UUID, ProjectCategory>()

    fun given(vararg categories: ProjectCategory) = categories.forEach { stored[it.id] = it }

    override fun save(category: ProjectCategory) = category.also { stored[it.id] = it }

    override fun findById(id: UUID) = stored[id]

    override fun findAllByProjectId(projectId: UUID) = stored.values.filter { it.projectId == projectId }

    override fun delete(id: UUID) {
        stored.remove(id)
    }
}

internal class InMemoryStatusRepository : ProjectStatusRepository {
    val stored = linkedMapOf<UUID, ProjectStatus>()

    fun given(vararg statuses: ProjectStatus) = statuses.forEach { stored[it.id] = it }

    override fun save(status: ProjectStatus) = status.also { stored[it.id] = it }

    override fun findById(id: UUID) = stored[id]

    override fun findAllByProjectId(projectId: UUID) = stored.values.filter { it.projectId == projectId }

    override fun delete(id: UUID) {
        stored.remove(id)
    }
}
