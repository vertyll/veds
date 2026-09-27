package com.vertyll.veds.shared.saga.engine

import com.vertyll.veds.shared.saga.SagaStepStatus
import com.vertyll.veds.shared.saga.engine.persistence.SagaStepRepositoryPort

internal class InMemoryStepRepository : SagaStepRepositoryPort<TestStep> {
    val stored = mutableListOf<TestStep>()
    private var nextId = 1L

    override fun save(step: TestStep): TestStep {
        val withId = step.id?.let { step } ?: step.copy(id = nextId++)
        stored.removeAll { it.id == withId.id }
        stored += withId
        return withId
    }

    override fun findOneById(id: Long) = stored.firstOrNull { it.id == id }

    override fun findBySagaId(sagaId: String) = stored.filter { it.sagaId == sagaId }

    override fun findBySagaIdAndStepName(
        sagaId: String,
        stepName: String,
    ) = stored.filter { it.sagaId == sagaId && it.stepName == stepName }

    override fun findBySagaIdAndStatus(
        sagaId: String,
        status: SagaStepStatus,
    ) = stored.filter { it.sagaId == sagaId && it.status == status }

    override fun findByStepNameAndStatus(
        stepName: String,
        status: SagaStepStatus,
    ) = stored.filter { it.stepName == stepName && it.status == status }

    override fun findByCompensationStepId(compensationStepId: Long) = stored.firstOrNull { it.compensationStepId == compensationStepId }
}
