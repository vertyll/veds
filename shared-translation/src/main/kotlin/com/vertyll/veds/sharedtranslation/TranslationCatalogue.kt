package com.vertyll.veds.sharedtranslation

/**
 * Everything one service contributes to the catalogue.
 */
data class TranslationCatalogue(
    /** The service that declares these keys. */
    val sourceService: String,
    /** The keys the service declares. */
    val definitions: List<TranslationKeyDefinition>,
)
