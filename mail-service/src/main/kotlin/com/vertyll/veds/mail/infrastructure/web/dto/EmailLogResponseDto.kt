package com.vertyll.veds.mail.infrastructure.web.dto

data class EmailLogResponseDto(
    val id: Long,
    val recipient: String,
    val subject: String,
    val templateName: String,
    val status: String,
    val errorMessage: String?,
    val createdAt: String,
    val sentAt: String?,
)
