package com.vertyll.veds.translation.application.command

data class CatalogueEntryCommand(
    val key: String,
    val description: String?,
    val defaultValues: Map<String, String>,
)
