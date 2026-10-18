package com.vertyll.veds.mail.application.dto

data class EmailResult(
    val recipient: String,
    val success: Boolean,
)
