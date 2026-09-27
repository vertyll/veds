package com.vertyll.veds.shared.saga.engine

import com.vertyll.veds.shared.saga.Saga
import com.vertyll.veds.shared.saga.SagaStatus
import java.time.Instant

/**
 * In-memory stand-ins for the saga log.
 *
 * The engine talks to ports, so its rules can be exercised without a database — which is the
 * point of the port in the first place. What these fixtures do *not* fake is the state machine
 * itself: `markCompleted` and friends behave exactly as a persisted aggregate would.
 */
internal data class TestSaga(
    override val id: String,
    override val type: String,
    override val status: SagaStatus,
    override val payload: String,
    override val lastError: String? = null,
    override val startedAt: Instant = Instant.now(),
    override val completedAt: Instant? = null,
    override val updatedAt: Instant = Instant.now(),
    override val version: Long? = null,
) : Saga<TestSaga> {
    /**
     * Every transition refreshes [updatedAt], exactly as `BaseSaga` does. The watchdog selects on
     * that column alone, so a fixture that let it go stale would report retries the real engine
     * never performs.
     */
    private fun transition(
        status: SagaStatus,
        lastError: String? = this.lastError,
        completed: Boolean = false,
    ): TestSaga {
        val now = Instant.now()
        return copy(
            status = status,
            lastError = lastError,
            completedAt = if (completed) now else completedAt,
            updatedAt = now,
        )
    }

    override fun markCompleted() = transition(SagaStatus.COMPLETED, completed = true)

    override fun markAwaitingResponse() = transition(SagaStatus.AWAITING_RESPONSE)

    override fun markFailed(error: String) = transition(SagaStatus.FAILED, lastError = error, completed = true)

    override fun startCompensating(error: String) = transition(SagaStatus.COMPENSATING, lastError = error)

    override fun markCompensated() = transition(SagaStatus.COMPENSATED, completed = true)

    override fun markCompensationFailed() = transition(SagaStatus.COMPENSATION_FAILED, completed = true)
}
