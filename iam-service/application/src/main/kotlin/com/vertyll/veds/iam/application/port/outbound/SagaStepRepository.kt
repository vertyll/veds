package com.vertyll.veds.iam.application.port.outbound

import com.vertyll.veds.iam.application.saga.model.SagaStep
import com.vertyll.veds.sharedinfrastructure.saga.enums.SagaStepStatus

interface SagaStepRepository {
    fun save(sagaStep: SagaStep): SagaStep

    fun findById(id: Long): SagaStep?

    fun findBySagaId(sagaId: String): List<SagaStep>

    fun findBySagaIdAndStepName(
        sagaId: String,
        stepName: String,
    ): SagaStep?

    fun findBySagaIdAndStepNameAndStatus(
        sagaId: String,
        stepName: String,
        status: SagaStepStatus,
    ): List<SagaStep>

    fun findBySagaIdOrderByCreatedAtDesc(sagaId: String): List<SagaStep>
}
