package com.vertyll.veds.translation.application.dto

data class ExportRowResponse(
    val key: String,
    val sourceService: String,
    val description: String?,
    val values: Map<String, String>,
    val defaultValues: Map<String, String>,
)
