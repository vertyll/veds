package com.vertyll.projecta.sharedinfrastructure.saga.entity

import com.vertyll.projecta.sharedinfrastructure.saga.enums.SagaStatus
import jakarta.persistence.Column
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.MappedSuperclass
import jakarta.persistence.Version
import java.time.Instant

@MappedSuperclass
abstract class BaseSaga(
    @Id
    open val id: String,
    @Column(nullable = false)
    open val type: String,
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    open var status: SagaStatus,
    @Column(nullable = false, columnDefinition = "TEXT")
    open val payload: String,
    @Column(nullable = true)
    open var lastError: String? = null,
    @Column(nullable = false)
    open val startedAt: Instant,
    @Column(nullable = true)
    open var completedAt: Instant? = null,
    @Column(nullable = false)
    open var updatedAt: Instant = Instant.now(),
    @Version
    open val version: Long? = null,
)
