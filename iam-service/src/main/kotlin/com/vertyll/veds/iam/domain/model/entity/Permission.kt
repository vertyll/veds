package com.vertyll.veds.iam.domain.model.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.Version
import java.time.Instant

@Entity
@Table(name = "permission")
class Permission(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
    @Column(nullable = false, unique = true)
    val name: String,
    @Column(nullable = true)
    val description: String? = null,
    @Column(nullable = false)
    val createdAt: Instant = Instant.now(),
    @Column(nullable = false)
    var updatedAt: Instant = Instant.now(),
    @Version
    val version: Long? = null,
) {
    constructor() : this(
        id = null,
        name = "",
        description = null,
        version = null,
    )

    companion object {
        fun create(
            name: String,
            description: String? = null,
        ): Permission =
            Permission(
                name = name,
                description = description,
            )
    }
}
