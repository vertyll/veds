package com.vertyll.veds.notification.infrastructure.persistence.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

@Entity
@Table(
    name = "recipient_ref",
    indexes = [Index(name = "idx_recipient_ref_email", columnList = "email")],
)
internal class RecipientRefJpaEntity(
    @Id
    @Column(name = "user_id", nullable = false, updatable = false)
    var userId: UUID,
    @Column(name = "email", nullable = false)
    var email: String,
    @Column(name = "display_name")
    var displayName: String? = null,
    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now(),
)
