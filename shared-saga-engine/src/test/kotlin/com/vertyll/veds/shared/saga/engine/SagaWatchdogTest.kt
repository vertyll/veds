package com.vertyll.veds.shared.saga.engine

import com.vertyll.veds.shared.saga.SagaStatus
import org.junit.jupiter.api.Test
import tools.jackson.databind.ObjectMapper
import java.time.Duration
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Nothing else notices a saga that stops receiving events — the participant that owed the next
 * message may simply never send it. The watchdog is the only thing standing between that and a
 * workflow that stays open forever, so it has to act on age alone and survive its own failures.
 */
internal class SagaWatchdogTest {
    private val sagas = InMemorySagaRepository()
    private val steps = InMemoryStepRepository()
    private val compensated = mutableListOf<String>()

    private val engine =
        SagaEngine(
            sagaRepository = sagas,
            sagaStepRepository = steps,
            objectMapper = ObjectMapper(),
            entityFactory = TestEntityFactory(),
            compensationRunner = { sagaId -> compensated += sagaId },
        )

    private val properties =
        SagaProperties(
            awaitResponseTimeout = Duration.ofMinutes(30),
            compensationRetryCooldown = Duration.ofMinutes(5),
        )

    private val watchdog = SagaWatchdog(sagas, engine, properties)

    private fun givenSaga(
        id: String,
        status: SagaStatus,
        idleFor: Duration,
    ) = sagas.save(
        TestSaga(
            id = id,
            type = "CreateProject",
            status = status,
            payload = "{}",
            updatedAt = Instant.now().minus(idleFor),
        ),
    )

    private fun statusOf(id: String) = sagas.findOneById(id)!!.status

    /**
     * Timing out is not the end state: the saga moves into COMPENSATING so its already-committed
     * steps get undone, and only the compensation run settles it.
     */
    @Test
    fun `a saga waiting past the timeout starts compensating`() {
        givenSaga("stale", SagaStatus.AWAITING_RESPONSE, idleFor = Duration.ofHours(2))

        watchdog.tick()

        assertEquals(SagaStatus.COMPENSATING, statusOf("stale"))
        assertEquals("timeout", sagas.findOneById("stale")!!.lastError)
    }

    /**
     * The timeout is what triggers the undo — a saga nobody answered must not leave its work
     * standing. It is compensated once: the transition refreshes the saga's age, so the same tick's
     * retry sweep no longer sees it as stuck.
     */
    @Test
    fun `timing a saga out compensates the steps it already ran`() {
        givenSaga("stale", SagaStatus.AWAITING_RESPONSE, idleFor = Duration.ofHours(2))

        watchdog.tick()

        assertEquals(listOf("stale"), compensated)
    }

    @Test
    fun `a saga still inside its timeout is left waiting`() {
        givenSaga("recent", SagaStatus.AWAITING_RESPONSE, idleFor = Duration.ofMinutes(5))

        watchdog.tick()

        assertEquals(SagaStatus.AWAITING_RESPONSE, statusOf("recent"))
        assertTrue(compensated.isEmpty())
    }

    /** An old saga that is not waiting on anyone is simply old; only AWAITING_RESPONSE can time out. */
    @Test
    fun `age alone does not fail a saga in another state`() {
        givenSaga("running", SagaStatus.STARTED, idleFor = Duration.ofDays(1))
        givenSaga("done", SagaStatus.COMPLETED, idleFor = Duration.ofDays(1))

        watchdog.tick()

        assertEquals(SagaStatus.STARTED, statusOf("running"))
        assertEquals(SagaStatus.COMPLETED, statusOf("done"))
    }

    /**
     * Compensation can fail against a service that is temporarily down. The saga is then left in
     * COMPENSATING or COMPENSATION_FAILED, and the watchdog is what eventually drives it home.
     */
    @Test
    fun `an unfinished compensation is retried once its cooldown has passed`() {
        givenSaga("half-undone", SagaStatus.COMPENSATION_FAILED, idleFor = Duration.ofMinutes(10))
        givenSaga("undoing", SagaStatus.COMPENSATING, idleFor = Duration.ofMinutes(10))

        watchdog.tick()

        assertEquals(listOf("half-undone", "undoing"), compensated.sorted())
    }

    /** Without the cooldown the watchdog would re-fire on a compensation that is still in progress. */
    @Test
    fun `a compensation inside its cooldown is not retried`() {
        givenSaga("undoing", SagaStatus.COMPENSATING, idleFor = Duration.ofMinutes(1))

        watchdog.tick()

        assertTrue(compensated.isEmpty())
    }

    /**
     * The watchdog is scheduled, so one bad saga must not cost every other saga its next sweep.
     */
    @Test
    fun `one saga failing does not stop the rest of the sweep`() {
        val exploding =
            object : SagaEngine<TestSaga, TestStep>(sagas, steps, ObjectMapper(), TestEntityFactory(), { }) {
                override fun runCompensation(sagaId: String) {
                    if (sagaId == "poison") error("participant unreachable")
                    compensated += sagaId
                }
            }
        givenSaga("poison", SagaStatus.COMPENSATING, idleFor = Duration.ofMinutes(10))
        givenSaga("healthy", SagaStatus.COMPENSATING, idleFor = Duration.ofMinutes(10))

        SagaWatchdog(sagas, exploding, properties).tick()

        assertEquals(listOf("healthy"), compensated)
    }
}
