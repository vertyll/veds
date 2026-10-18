package com.vertyll.veds.task.infrastructure.persistence.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "user_ref")
internal class UserRefJpaEntity(
    @Id
    @Column(name = "user_id", nullable = false, updatable = false)
    var userId: UUID,
    @Column(name = "email", nullable = false)
    var email: String,
    @Column(name = "first_name")
    var firstName: String? = null,
    @Column(name = "last_name")
    var lastName: String? = null,
    @Column(name = "avatar_file_id")
    var avatarFileId: UUID? = null,
    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now(),
)
