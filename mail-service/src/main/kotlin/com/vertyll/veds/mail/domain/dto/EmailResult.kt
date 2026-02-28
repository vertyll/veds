package com.vertyll.veds.mail.domain.dto

data class EmailResult(
    val recipient: String,
    val success: Boolean,
)
