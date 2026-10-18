package com.vertyll.veds.translation.application.dto

import java.time.Instant

data class TranslationKeyDetailsResponse(
    val key: String,
    val sourceService: String,
    val description: String?,
    val values: List<TranslationValueResponse>,
    val missingLanguages: List<String>,
    val createdAt: Instant,
    val updatedAt: Instant,
)
