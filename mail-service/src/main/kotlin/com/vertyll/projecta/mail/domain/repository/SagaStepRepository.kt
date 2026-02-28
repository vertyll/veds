package com.vertyll.projecta.mail.domain.repository

import com.vertyll.projecta.mail.domain.model.entity.SagaStep
import com.vertyll.projecta.sharedinfrastructure.saga.enums.SagaStepStatus
import com.vertyll.projecta.sharedinfrastructure.saga.repository.BaseSagaStepRepository
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
