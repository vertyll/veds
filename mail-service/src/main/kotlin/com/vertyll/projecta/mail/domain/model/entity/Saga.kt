package com.vertyll.projecta.mail.domain.model.entity

import com.vertyll.projecta.mail.domain.model.enums.SagaStatus
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Lob
import jakarta.persistence.Table
import java.time.Instant

@Entity
@Table(name = "saga")
class Saga(
    @Id
    val id: String,
    @Column(nullable = false)
    val type: String,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: SagaStatus = SagaStatus.STARTED,
    @Lob
    @Column(columnDefinition = "TEXT")
    val payload: String?,
    @Column(nullable = false)
    val startedAt: Instant = Instant.now(),
    @Column(nullable = true)
    var completedAt: Instant? = null,
    @Column(nullable = true)
    var lastError: String? = null,
    @Column(nullable = false)
    var updatedAt: Instant = Instant.now(),
) {
    constructor() : this(
        id = "",
        type = "",
        status = SagaStatus.STARTED,
        payload = null,
        startedAt = Instant.now(),
        completedAt = null,
    )
}
