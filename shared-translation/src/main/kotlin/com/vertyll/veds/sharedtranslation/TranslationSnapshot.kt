package com.vertyll.veds.sharedtranslation

/**
 * An immutable set of translations for one language.
 *
 * @property language the language of the set.
 * @property version served as an ETag, so a client already holding the current
 *           set confirms it with a 304 instead of transferring everything again.
 * @property entries the patterns, by key.
 */
data class TranslationSnapshot(
    val language: String,
    val version: String,
    val entries: Map<String, String>,
) : TranslationSource {
    override fun patternFor(
        key: String,
        language: String,
    ): String? = if (language.equals(this.language, ignoreCase = true)) entries[key] else null

    /** Builds the snapshot a service starts from. */
    companion object {
        /** The version of a snapshot that holds nothing yet. */
        const val EMPTY_VERSION = "0"

        /** A snapshot of [language] that holds no messages. */
        fun empty(language: String) = TranslationSnapshot(language, EMPTY_VERSION, emptyMap())
    }
}
