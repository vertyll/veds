package com.vertyll.veds.translation.application.command

data class ImportedTranslationCommand(
    val key: String,
    val language: String,
    val value: String,
)
