package com.vertyll.veds.iam.infrastructure.persistence.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
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
        Index(name = "idx_user_email", columnList = "email"),
        Index(name = "idx_user_keycloak_id", columnList = "keycloak_id"),
    ],
)
internal class UserJpaEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,
    @Column(name = "keycloak_id", nullable = true, unique = true)
    var keycloakId: UUID? = null,
    @Column(nullable = false, unique = true)
    var email: String,
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
    var roles: MutableSet<RoleJpaEntity> = mutableSetOf(),
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "user_permission_mapping",
        joinColumns = [JoinColumn(name = "user_id")],
        inverseJoinColumns = [JoinColumn(name = "permission_id")],
    )
    var permissions: MutableSet<PermissionJpaEntity> = mutableSetOf(),
    @Column(nullable = true)
    var profilePicture: String? = null,
    @Column(nullable = true)
    var phoneNumber: String? = null,
    @Column(nullable = true)
    var address: String? = null,
    @Column(nullable = false)
    var createdAt: Instant = Instant.now(),
    @Column(nullable = false)
    var updatedAt: Instant = Instant.now(),
    @Version
    var version: Long? = null,
)
