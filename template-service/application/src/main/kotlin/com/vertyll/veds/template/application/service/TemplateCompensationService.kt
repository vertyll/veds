package com.vertyll.veds.template.application.service

import com.vertyll.veds.template.application.port.inbound.TemplateCompensationUseCase
import com.vertyll.veds.template.application.saga.model.TemplateCompensationCommand
import com.vertyll.veds.template.domain.repository.TemplateRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Reference compensation use case for the template bounded context.
 *
 * Mirrors `AuthCompensationService` in iam-service. Dispatches via an
 * exhaustive `when` over the sealed [TemplateCompensationCommand] — so
 * adding a new compensation variant becomes a compile-time error.
 *
 * Exceptions are intentionally propagated to the calling Kafka adapter
 * so failures land in DLT after the configured retry budget, while the
 * `SagaWatchdog` keeps providing a slower cooldown-based safety net for
 * sagas stuck in `COMPENSATING` / `COMPENSATION_FAILED`.
 *
 * Implementations here are placeholders intended to be replaced when
 * cloning the template into a real service.
 */
@Service
internal class TemplateCompensationService(
    private val templateRepository: TemplateRepository,
) : TemplateCompensationUseCase {
    private val logger = LoggerFactory.getLogger(javaClass)

    @Transactional
    override fun compensate(command: TemplateCompensationCommand) {
        when (command) {
            is TemplateCompensationCommand.DeleteTemplate -> deleteTemplate(command.templateId)
            is TemplateCompensationCommand.LogTemplateCompensation -> logCompensation(command.templateId)
        }
    }

    private fun deleteTemplate(templateId: String) {
        logger.info("Compensating PersistTemplate — deleting template {}", templateId)
        templateRepository.findById(templateId)?.let { templateRepository.deleteById(it.id) }
    }

    private fun logCompensation(templateId: String) {
        // Placeholder: a published Kafka event cannot be un-published.
        // When cloning this service, replace with the appropriate
        // domain-level compensation (e.g. publish a reversal event).
        logger.info(
            "Compensating PublishTemplateEvent for template {} — no externally-observable rollback possible",
            templateId,
        )
    }
}
