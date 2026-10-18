package com.vertyll.veds.translation.application.dto

data class ImportReportResponse(
    val applied: Int,
    val skippedUnknownKeys: List<String>,
    val skippedUnknownLanguages: List<String>,
    val rejectedPatterns: List<RejectedPatternResponse>,
    val missingAfterImport: List<MissingTranslationResponse>,
)
