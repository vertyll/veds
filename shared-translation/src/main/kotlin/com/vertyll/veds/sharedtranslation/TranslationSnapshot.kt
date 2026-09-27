package com.vertyll.veds.sharedtranslation

/**
 * An immutable set of translations for one language.
 *
 * @property version served as an ETag, so a client already holding the current
 *           set confirms it with a 304 instead of transferring everything again.
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

    companion object {
        fun empty(language: String) = TranslationSnapshot(language, EMPTY_VERSION, emptyMap())

        const val EMPTY_VERSION = "0"
    }
}
