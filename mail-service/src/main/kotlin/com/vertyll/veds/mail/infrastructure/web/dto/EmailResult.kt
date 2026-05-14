package com.vertyll.veds.mail.infrastructure.web.dto

data class EmailResult(
    val recipient: String,
    val success: Boolean,
)
