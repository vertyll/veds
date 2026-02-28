package com.vertyll.projecta.template.domain.model.entity

import com.vertyll.projecta.template.domain.model.enums.SagaStepStatus
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant

/**
 * Represents a step in a saga (a distributed transaction across multiple services).
 */
@Entity
@Table(name = "saga_step")
class SagaStep(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
    @Column(nullable = false)
    val sagaId: String,
    @Column(nullable = false)
    val stepName: String,
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    var status: SagaStepStatus,
    @Column(nullable = true, columnDefinition = "TEXT")
    val payload: String? = null,
    @Column(nullable = true)
    var errorMessage: String? = null,
    @Column(nullable = false)
    val createdAt: Instant,
    @Column(nullable = true)
    var completedAt: Instant? = null,
    @Column(nullable = true)
    var compensationStepId: Long? = null,
) {
    constructor() : this(
        id = null,
        sagaId = "",
        stepName = "",
        status = SagaStepStatus.STARTED,
        payload = null,
        errorMessage = null,
        createdAt = Instant.now(),
        completedAt = null,
        compensationStepId = null,
    )
}
