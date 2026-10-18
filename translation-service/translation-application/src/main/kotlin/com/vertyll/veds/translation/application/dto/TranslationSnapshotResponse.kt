package com.vertyll.veds.translation.application.dto

data class TranslationSnapshotResponse(
    val language: String,
    val version: String,
    val entries: Map<String, String>,
)
