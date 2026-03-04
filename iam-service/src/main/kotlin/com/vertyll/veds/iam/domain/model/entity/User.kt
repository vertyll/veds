package com.vertyll.veds.iam.domain.model.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.JoinTable
import jakarta.persistence.ManyToMany
import jakarta.persistence.Table
import jakarta.persistence.Version
import java.time.Instant
import java.util.UUID

@Entity
@Table(
    name = "\"user\"",
    indexes = [
        jakarta.persistence.Index(name = "idx_user_email", columnList = "email"),
        jakarta.persistence.Index(name = "idx_user_keycloak_id", columnList = "keycloak_id"),
    ],
)
class User(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
    @Column(name = "keycloak_id", nullable = true, unique = true)
    var keycloakId: UUID? = null,
    @Column(nullable = false, unique = true)
    private var email: String,
    @Column(nullable = false)
    var firstName: String,
    @Column(nullable = false)
    var lastName: String,
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "user_role_mapping",
        joinColumns = [JoinColumn(name = "user_id")],
        inverseJoinColumns = [JoinColumn(name = "role_id")],
    )
    var roles: MutableSet<Role> = mutableSetOf(),
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "user_permission_mapping",
        joinColumns = [JoinColumn(name = "user_id")],
        inverseJoinColumns = [JoinColumn(name = "permission_id")],
    )
    var permissions: MutableSet<Permission> = mutableSetOf(),
    @Column(nullable = true)
    var profilePicture: String? = null,
    @Column(nullable = true)
    var phoneNumber: String? = null,
    @Column(nullable = true)
    var address: String? = null,
    @Column(nullable = false)
    val createdAt: Instant = Instant.now(),
    @Column(nullable = false)
    var updatedAt: Instant = Instant.now(),
    @Version
    val version: Long? = null,
) {
    constructor() : this(
        id = null,
        keycloakId = null,
        email = "",
        firstName = "",
        lastName = "",
        roles = mutableSetOf(),
        permissions = mutableSetOf(),
        version = null,
    )

    fun getEmail(): String = email

    fun setEmail(newEmail: String) {
        this.email = newEmail
        this.updatedAt = Instant.now()
    }

    fun addRole(role: Role) {
        if (role.id != null && roles.none { it.id == role.id }) {
            roles.add(role)
            updatedAt = Instant.now()
        }
    }

    fun removeRole(roleId: Long) {
        val roleToRemove = roles.find { it.id == roleId }
        if (roleToRemove != null) {
            roles.remove(roleToRemove)
            updatedAt = Instant.now()
        }
    }

    fun addPermission(permission: Permission) {
        if (permission.id != null && permissions.none { it.id == permission.id }) {
            permissions.add(permission)
            updatedAt = Instant.now()
        }
    }

    fun removePermission(permissionId: Long) {
        val permissionToRemove = permissions.find { it.id == permissionId }
        if (permissionToRemove != null) {
            permissions.remove(permissionToRemove)
            updatedAt = Instant.now()
        }
    }

    @Suppress("kotlin:S107")
    companion object {
        fun create(
            keycloakId: UUID,
            email: String,
            firstName: String,
            lastName: String,
            profilePicture: String? = null,
            phoneNumber: String? = null,
            address: String? = null,
        ): User =
            User(
                keycloakId = keycloakId,
                email = email,
                firstName = firstName,
                lastName = lastName,
                profilePicture = profilePicture,
                phoneNumber = phoneNumber,
                address = address,
            )
    }
}
