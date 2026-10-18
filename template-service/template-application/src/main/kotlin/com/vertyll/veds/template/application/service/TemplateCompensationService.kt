package com.vertyll.veds.template.application.service

import com.vertyll.veds.template.application.port.inbound.TemplateCompensationUseCase
import com.vertyll.veds.template.application.port.outbound.UseCaseLogger
import com.vertyll.veds.template.application.saga.model.TemplateCompensationCommand
import com.vertyll.veds.template.domain.repository.TemplateRepository

class TemplateCompensationService(
    private val templateRepository: TemplateRepository,
    private val logger: UseCaseLogger,
) : TemplateCompensationUseCase {
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
        logger.info(
            "Compensating PublishTemplateEvent for template {} — no externally-observable rollback possible",
            templateId,
        )
    }
}
