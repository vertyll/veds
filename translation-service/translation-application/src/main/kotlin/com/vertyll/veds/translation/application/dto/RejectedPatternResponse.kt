package com.vertyll.veds.translation.application.dto

data class RejectedPatternResponse(
    val key: String,
    val language: String,
    val reason: String,
)
