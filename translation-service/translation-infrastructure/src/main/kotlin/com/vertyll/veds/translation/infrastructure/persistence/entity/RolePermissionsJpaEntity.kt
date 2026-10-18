package com.vertyll.veds.translation.infrastructure.persistence.entity

import jakarta.persistence.CollectionTable
import jakarta.persistence.Column
import jakarta.persistence.ElementCollection
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.Table
import java.time.Instant

@Entity
@Table(name = "role_permission_projection")
internal class RolePermissionsJpaEntity(
    @Id
    @Column(name = "role_name", nullable = false, length = 64)
    var roleName: String = "",
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
        name = "role_permission_projection_permission",
        joinColumns = [JoinColumn(name = "role_name")],
    )
    @Column(name = "permission", nullable = false, length = 128)
    var permissions: MutableSet<String> = mutableSetOf(),
    @Column(name = "unrestricted", nullable = false)
    var unrestricted: Boolean = false,
    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now(),
)
