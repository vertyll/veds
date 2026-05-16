package com.vertyll.veds.iam.infrastructure.persistence.adapter

import com.vertyll.veds.iam.domain.model.PageResult
import com.vertyll.veds.iam.domain.model.User
import com.vertyll.veds.iam.domain.repository.UserRepository
import com.vertyll.veds.iam.infrastructure.persistence.entity.PermissionJpaEntity
import com.vertyll.veds.iam.infrastructure.persistence.entity.RoleJpaEntity
import com.vertyll.veds.iam.infrastructure.persistence.entity.UserJpaEntity
import com.vertyll.veds.iam.infrastructure.persistence.repository.PermissionJpaRepository
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
    private val permissionJpaRepository: PermissionJpaRepository,
) : UserRepository {
    override fun save(user: User): User {
        val managedRoles: MutableSet<RoleJpaEntity> =
            user.roles
                .mapNotNull { it.id?.let { id -> roleJpaRepository.findById(id).orElse(null) } }
                .toMutableSet()
        val managedPermissions: MutableSet<PermissionJpaEntity> =
            user.permissions
                .mapNotNull { it.id?.let { id -> permissionJpaRepository.findById(id).orElse(null) } }
                .toMutableSet()

        val entity =
            UserJpaEntity(
                id = user.id,
                keycloakId = user.keycloakId,
                email = user.email,
                firstName = user.firstName,
                lastName = user.lastName,
                roles = managedRoles,
                permissions = managedPermissions,
                profilePicture = user.profilePicture,
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
        roles = this.roles.map { it.toDomain() }.toSet(),
        permissions = this.permissions.map { it.toDomain() }.toSet(),
        profilePicture = this.profilePicture,
        phoneNumber = this.phoneNumber,
        address = this.address,
        createdAt = this.createdAt,
        updatedAt = this.updatedAt,
        version = this.version,
    )
