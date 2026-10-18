package com.vertyll.veds.translation.application.dto

data class LanguageResponse(
    val tag: String,
    val displayName: String,
    val isDefault: Boolean,
)
