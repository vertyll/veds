package com.vertyll.veds.mail.application.command

data class SendBatchEmailCommand(
    val recipients: List<String>,
    val subject: String,
    val templateName: String,
    val replyTo: String?,
)
