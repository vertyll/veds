package com.vertyll.veds.iam.infrastructure.persistence.adapter

import com.vertyll.veds.iam.domain.model.PageResult
import com.vertyll.veds.iam.domain.model.User
import com.vertyll.veds.iam.domain.repository.UserRepository
import com.vertyll.veds.iam.infrastructure.persistence.entity.RoleJpaEntity
import com.vertyll.veds.iam.infrastructure.persistence.entity.UserJpaEntity
import com.vertyll.veds.iam.infrastructure.persistence.repository.RoleJpaRepository
import com.vertyll.veds.iam.infrastructure.persistence.repository.UserJpaRepository
import org.springframework.stereotype.Component
import java.util.UUID
import com.vertyll.veds.iam.domain.model.PageRequest as DomainPageRequest
import org.springframework.data.domain.PageRequest as SpringPageRequest

@Component
internal class UserPersistenceAdapter(
    private val repository: UserJpaRepository,
    private val roleJpaRepository: RoleJpaRepository,
) : UserRepository {
    override fun save(user: User): User {
        val managedRoles: MutableSet<RoleJpaEntity> = roleJpaRepository.findByNameIn(user.roles).toMutableSet()
        val missing = user.roles - managedRoles.mapTo(mutableSetOf()) { it.name }
        check(missing.isEmpty()) { "cannot assign roles that do not exist to a user: $missing" }
        val entity =
            UserJpaEntity(
                id = user.id,
                keycloakId = user.keycloakId,
                email = user.email,
                firstName = user.firstName,
                lastName = user.lastName,
                roles = managedRoles,
                avatarFileId = user.avatarFileId,
                phoneNumber = user.phoneNumber,
                address = user.address,
                createdAt = user.createdAt,
                updatedAt = user.updatedAt,
                version = user.version,
            )
        return repository.save(entity).toDomain()
    }

    override fun findById(id: Long): User? = repository.findById(id).orElse(null)?.toDomain()

    override fun findByEmail(email: String): User? = repository.findByEmail(email).orElse(null)?.toDomain()

    override fun findByKeycloakId(keycloakId: UUID): User? = repository.findByKeycloakId(keycloakId).orElse(null)?.toDomain()

    override fun existsByEmail(email: String): Boolean = repository.existsByEmail(email)

    override fun findAll(pageRequest: DomainPageRequest): PageResult<User> {
        val springPage =
            repository.findAll(SpringPageRequest.of(pageRequest.page, pageRequest.size))
        return PageResult(
            content = springPage.content.map { it.toDomain() },
            page = pageRequest.page,
            size = pageRequest.size,
            totalElements = springPage.totalElements,
        )
    }

    override fun search(
        term: String,
        pageRequest: DomainPageRequest,
    ): PageResult<User> {
        val springPage =
            repository.findByEmailContainingIgnoreCaseOrFirstNameContainingIgnoreCaseOrLastNameContainingIgnoreCase(
                email = term,
                firstName = term,
                lastName = term,
                pageable = SpringPageRequest.of(pageRequest.page, pageRequest.size),
            )
        return PageResult(
            content = springPage.content.map { it.toDomain() },
            page = pageRequest.page,
            size = pageRequest.size,
            totalElements = springPage.totalElements,
        )
    }

    override fun countByRole(roleName: String): Long = repository.countByRoleName(roleName)

    override fun deleteById(id: Long) {
        repository.deleteById(id)
    }
}

private fun UserJpaEntity.toDomain(): User =
    User(
        id = this.id,
        keycloakId = this.keycloakId,
        email = this.email,
        firstName = this.firstName,
        lastName = this.lastName,
        roles = this.roles.mapTo(mutableSetOf()) { it.name },
        avatarFileId = this.avatarFileId,
        phoneNumber = this.phoneNumber,
        address = this.address,
        createdAt = this.createdAt,
        updatedAt = this.updatedAt,
        version = this.version,
    )
