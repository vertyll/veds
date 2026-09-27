package com.vertyll.veds.shared.saga.engine

import com.vertyll.veds.shared.saga.SagaStatus
import com.vertyll.veds.shared.saga.engine.persistence.SagaRepositoryPort
import java.time.Instant

internal class InMemorySagaRepository : SagaRepositoryPort<TestSaga> {
    val stored = linkedMapOf<String, TestSaga>()

    override fun save(saga: TestSaga): TestSaga = saga.also { stored[it.id] = it }

    override fun findOneById(id: String): TestSaga? = stored[id]

    override fun findByType(type: String) = stored.values.filter { it.type == type }

    override fun findByStatus(status: SagaStatus) = stored.values.filter { it.status == status }

    override fun findByTypeAndStatus(
        type: String,
        status: SagaStatus,
    ) = stored.values.filter { it.type == type && it.status == status }

    override fun findByStartedAtBefore(startedAt: Instant) = stored.values.filter { it.startedAt < startedAt }

    override fun findByStatusInAndStartedAtBefore(
        statuses: List<SagaStatus>,
        startedAt: Instant,
    ) = stored.values.filter { it.status in statuses && it.startedAt < startedAt }

    override fun findByStatusInAndUpdatedAtBefore(
        statuses: List<SagaStatus>,
        updatedAt: Instant,
    ) = stored.values.filter { it.status in statuses && it.updatedAt < updatedAt }
}
