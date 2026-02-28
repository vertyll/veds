package com.vertyll.veds.mail.domain.repository

import com.vertyll.veds.mail.domain.model.entity.SagaStep
import com.vertyll.veds.sharedinfrastructure.saga.enums.SagaStepStatus
import com.vertyll.veds.sharedinfrastructure.saga.repository.BaseSagaStepRepository
import org.springframework.stereotype.Repository

@Repository
interface SagaStepRepository : BaseSagaStepRepository<SagaStep> {
    fun findBySagaIdAndStepNameAndStatus(
        sagaId: String,
        stepName: String,
        status: SagaStepStatus,
    ): List<SagaStep>

    fun findBySagaIdOrderByCreatedAtDesc(sagaId: String): List<SagaStep>
}
