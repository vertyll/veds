package com.vertyll.veds.template.application.service

import com.vertyll.veds.sharedinfrastructure.saga.enums.SagaStepStatus
import com.vertyll.veds.template.application.saga.model.SagaStepNames
import com.vertyll.veds.template.application.saga.model.SagaTypes
import com.vertyll.veds.template.application.saga.port.SagaProcessPort
import com.vertyll.veds.template.domain.model.Template
import com.vertyll.veds.template.domain.repository.TemplateRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Reference implementation of a saga-driven use case for the template service.
 *
 * Mirrors the structure of `EmailSagaService` in mail-service — replace the
 * domain calls with your real business logic when cloning this service.
 */
@Service
class TemplateSagaService(
    private val sagaProcess: SagaProcessPort,
    private val templateRepository: TemplateRepository,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    @Transactional
    fun processTemplateWithSaga(
        name: String,
        payload: String,
    ): Template {
        val sagaId =
            sagaProcess
                .startSaga(
                    sagaType = SagaTypes.TEMPLATE_PROCESSING,
                    payload = mapOf("name" to name),
                ).id

        return try {
            sagaProcess.recordSagaStep(
                sagaId = sagaId,
                stepName = SagaStepNames.PROCESS_TEMPLATE,
                status = SagaStepStatus.COMPLETED,
                payload = mapOf("name" to name),
            )

            val saved = templateRepository.save(Template(name = name, payload = payload))

            sagaProcess.recordSagaStep(
                sagaId = sagaId,
                stepName = SagaStepNames.PERSIST_TEMPLATE,
                status = SagaStepStatus.COMPLETED,
                payload = mapOf("templateId" to saved.id),
            )

            val processed = templateRepository.save(saved.markProcessed())

            sagaProcess.markSagaCompleted(sagaId)
            processed
        } catch (e: Exception) {
            logger.error("Template saga failed: ${e.message}", e)
            sagaProcess.recordSagaStep(
                sagaId = sagaId,
                stepName = SagaStepNames.PERSIST_TEMPLATE,
                status = SagaStepStatus.FAILED,
                payload = mapOf("error" to (e.message ?: "Unknown error")),
            )
            sagaProcess.markSagaFailed(sagaId, e.message ?: "Unknown error")
            throw e
        }
    }
}
