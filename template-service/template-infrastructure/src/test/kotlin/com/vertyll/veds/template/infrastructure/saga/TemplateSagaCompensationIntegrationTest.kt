package com.vertyll.veds.template.infrastructure.saga

import com.vertyll.veds.shared.messaging.kafka.persistence.outbox.OutboxRepository
import com.vertyll.veds.shared.saga.SagaProcessPort
import com.vertyll.veds.shared.saga.SagaStatus
import com.vertyll.veds.shared.saga.SagaStepStatus
import com.vertyll.veds.sharederror.ApiException
import com.vertyll.veds.template.application.command.CreateTemplateCommand
import com.vertyll.veds.template.application.port.inbound.command.TemplateCommandUseCase
import com.vertyll.veds.template.application.port.inbound.query.TemplateQueryUseCase
import com.vertyll.veds.template.application.port.outbound.TemplateEventPublisherPort
import com.vertyll.veds.template.application.saga.model.SagaStepNames
import com.vertyll.veds.template.application.saga.model.SagaTypes
import com.vertyll.veds.template.domain.repository.TemplateRepository
import com.vertyll.veds.template.infrastructure.IntegrationTestBase
import com.vertyll.veds.template.infrastructure.kafka.TemplateKafkaTopics
import org.awaitility.Awaitility.await
import org.junit.jupiter.api.Test
import java.time.Duration
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

internal class TemplateSagaCompensationIntegrationTest(
    private val commands: TemplateCommandUseCase,
    private val queries: TemplateQueryUseCase,
    private val templates: TemplateRepository,
    private val sagaProcess: SagaProcessPort,
    private val publisher: TemplateEventPublisherPort,
    private val outbox: OutboxRepository,
) : IntegrationTestBase() {
    @Test
    fun `a failed saga deletes the template it had persisted`() {
        val template = commands.processTemplateWithSaga(CreateTemplateCommand(name = "Doomed", payload = "body"))
        val sagaId = sagaProcess.startSaga(SagaTypes.TEMPLATE_PROCESSING, mapOf("name" to "Doomed")).id
        sagaProcess.recordSagaStep(
            sagaId = sagaId,
            stepName = SagaStepNames.PROCESS_TEMPLATE,
            status = SagaStepStatus.COMPLETED,
        )
        sagaProcess.recordSagaStep(
            sagaId = sagaId,
            stepName = SagaStepNames.PERSIST_TEMPLATE,
            status = SagaStepStatus.COMPLETED,
            payload = mapOf("templateId" to template.id),
        )
        sagaProcess.recordSagaStep(
            sagaId = sagaId,
            stepName = SagaStepNames.PUBLISH_TEMPLATE_EVENT,
            status = SagaStepStatus.COMPLETED,
            payload = mapOf("templateId" to template.id),
        )

        sagaProcess.markSagaFailed(sagaId, "the publication was refused")

        await().atMost(TIMEOUT).until { templates.findById(template.id) == null }
        val saga = assertNotNull(sagaProcess.findSagaById(sagaId))
        assertTrue(saga.status != SagaStatus.COMPLETED, "a compensated saga must not read as completed")
    }

    @Test
    fun `a stored template is read back and a missing one is refused`() {
        val template = commands.processTemplateWithSaga(CreateTemplateCommand(name = "Kept", payload = "body"))

        assertEquals("Kept", queries.getById(template.id).name)
        assertFailsWith<ApiException> { queries.getById("no-such-template") }
    }

    @Test
    fun `a processed template is announced through the outbox`() {
        publisher.publishTemplateProcessed("saga-processed", 7L, mapOf("name" to "Announced"))

        val queued = outbox.findAll().filter { it.sagaId == "saga-processed" }
        assertEquals(listOf(TemplateKafkaTopics.TEMPLATE_PROCESSED), queued.map { it.topic })
    }

    @Test
    fun `a failed template is announced through the outbox`() {
        publisher.publishTemplateFailed("saga-failed", null, "refused")

        val queued = outbox.findAll().filter { it.sagaId == "saga-failed" }
        assertEquals(listOf(TemplateKafkaTopics.TEMPLATE_FAILED), queued.map { it.topic })
    }

    private companion object {
        val TIMEOUT: Duration = Duration.ofSeconds(30)
    }
}
