package com.vertyll.veds.mail.application.saga.model

sealed interface MailCompensationCommand {
    data class LogEmailCompensation(
        val emailId: String,
        val to: String,
    ) : MailCompensationCommand

    data class DeleteEmailLog(
        val logId: Long,
    ) : MailCompensationCommand

    data class LogTemplateCompensation(
        val templateName: String,
    ) : MailCompensationCommand
}
