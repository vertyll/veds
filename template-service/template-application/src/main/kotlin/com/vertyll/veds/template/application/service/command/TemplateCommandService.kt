package com.vertyll.veds.template.application.service.command

import com.vertyll.veds.shared.saga.SagaProcessPort
import com.vertyll.veds.shared.saga.SagaStepStatus
import com.vertyll.veds.template.application.command.CreateTemplateCommand
import com.vertyll.veds.template.application.port.inbound.command.TemplateCommandUseCase
import com.vertyll.veds.template.application.port.outbound.UseCaseLogger
import com.vertyll.veds.template.application.saga.model.SagaStepNames
import com.vertyll.veds.template.application.saga.model.SagaTypes
import com.vertyll.veds.template.domain.model.Template
import com.vertyll.veds.template.domain.repository.TemplateRepository

class TemplateCommandService(
    private val sagaProcess: SagaProcessPort,
    private val templateRepository: TemplateRepository,
    private val logger: UseCaseLogger,
) : TemplateCommandUseCase {
    override fun processTemplateWithSaga(command: CreateTemplateCommand): Template {
        val name = command.name
        val payload = command.payload
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
