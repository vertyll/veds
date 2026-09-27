package com.vertyll.veds.sharedtranslation

/**
 * Everything one service contributes to the catalogue.
 */
data class TranslationCatalogue(
    val sourceService: String,
    val definitions: List<TranslationKeyDefinition>,
)
