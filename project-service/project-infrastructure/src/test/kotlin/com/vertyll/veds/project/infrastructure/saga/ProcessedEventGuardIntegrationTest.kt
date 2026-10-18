@file:OptIn(ExperimentalUuidApi::class)

package com.vertyll.veds.project.infrastructure.saga

import com.vertyll.veds.project.infrastructure.IntegrationTestBase
import com.vertyll.veds.shared.messaging.kafka.ProcessedEventGuard
import com.vertyll.veds.shared.messaging.kafka.contract.ProcessedEventRepositoryPort
import org.junit.jupiter.api.Test
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.TransactionTemplate
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

class ProcessedEventGuardIntegrationTest(
    private val guard: ProcessedEventGuard,
    private val repository: ProcessedEventRepositoryPort,
    private val transactionManager: PlatformTransactionManager,
) : IntegrationTestBase() {
    private val group = "project-service:test"

    private fun inTransaction(block: () -> Unit) = TransactionTemplate(transactionManager).executeWithoutResult { block() }

    @Test
    fun `a failed handler leaves no claim, so the redelivery is a retry`() {
        val eventId = Uuid.generateV7().toString()

        runCatching {
            inTransaction {
                assertTrue(guard.claim(eventId, group), "the first delivery must be claimed")
                error("handler failed")
            }
        }

        assertFalse(repository.exists(eventId, group), "a rolled-back handler must not leave the event claimed")
        inTransaction {
            assertTrue(guard.claim(eventId, group), "the redelivery must be processed, not skipped as a duplicate")
        }
    }

    @Test
    fun `a committed handler keeps its claim, so a duplicate is skipped`() {
        val eventId = Uuid.generateV7().toString()

        inTransaction { assertTrue(guard.claim(eventId, group)) }

        assertTrue(repository.exists(eventId, group))
        inTransaction { assertFalse(guard.claim(eventId, group), "a genuine duplicate must still be absorbed") }
    }
}
