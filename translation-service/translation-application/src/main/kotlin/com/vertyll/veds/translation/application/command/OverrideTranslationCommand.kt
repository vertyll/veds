package com.vertyll.veds.translation.application.command

data class OverrideTranslationCommand(
    val key: String,
    val language: String,
    val value: String,
)
