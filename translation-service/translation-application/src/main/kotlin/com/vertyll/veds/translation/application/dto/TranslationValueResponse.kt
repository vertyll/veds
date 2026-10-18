package com.vertyll.veds.translation.application.dto

import java.time.Instant

data class TranslationValueResponse(
    val language: String,
    val defaultValue: String?,
    val overrideValue: String?,
    val effectiveValue: String?,
    val isOverridden: Boolean,
    val updatedAt: Instant,
    val version: Long?,
)
