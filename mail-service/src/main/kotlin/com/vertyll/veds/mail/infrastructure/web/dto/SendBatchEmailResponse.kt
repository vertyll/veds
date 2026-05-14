package com.vertyll.veds.mail.infrastructure.web.dto

data class SendBatchEmailResponse(
    val totalRecipients: Int,
    val successCount: Int,
    val failureCount: Int,
    val details: List<EmailResult>,
)
