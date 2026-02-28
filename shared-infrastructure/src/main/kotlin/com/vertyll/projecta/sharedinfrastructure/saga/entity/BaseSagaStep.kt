package com.vertyll.projecta.sharedinfrastructure.saga.entity

import com.vertyll.projecta.sharedinfrastructure.saga.enums.SagaStepStatus
import jakarta.persistence.Column
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.MappedSuperclass
import jakarta.persistence.Version
import java.time.Instant

@MappedSuperclass
abstract class BaseSagaStep(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    open val id: Long? = null,
    @Column(nullable = false)
    open val sagaId: String,
    @Column(nullable = false)
    open val stepName: String,
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    open var status: SagaStepStatus,
    @Column(nullable = true, columnDefinition = "TEXT")
    open val payload: String? = null,
    @Column(nullable = true)
    open var errorMessage: String? = null,
    @Column(nullable = false)
    open val createdAt: Instant,
    @Column(nullable = true)
    open var completedAt: Instant? = null,
    @Column(nullable = true)
    open var compensationStepId: Long? = null,
    @Version
    open val version: Long? = null,
)
