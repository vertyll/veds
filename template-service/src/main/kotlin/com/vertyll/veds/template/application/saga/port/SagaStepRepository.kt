package com.vertyll.veds.template.application.saga.port

import com.vertyll.veds.sharedinfrastructure.saga.enums.SagaStepStatus
import com.vertyll.veds.template.application.saga.model.SagaStep

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
