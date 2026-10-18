package com.vertyll.veds.translation.application.command

data class RegisterCatalogueCommand(
    val sourceService: String,
    val entries: List<CatalogueEntryCommand>,
)
