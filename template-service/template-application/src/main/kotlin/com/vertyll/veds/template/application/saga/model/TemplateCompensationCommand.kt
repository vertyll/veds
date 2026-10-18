package com.vertyll.veds.template.application.saga.model

sealed interface TemplateCompensationCommand {
    data class DeleteTemplate(
        val templateId: String,
    ) : TemplateCompensationCommand

    data class LogTemplateCompensation(
        val templateId: String,
    ) : TemplateCompensationCommand
}
