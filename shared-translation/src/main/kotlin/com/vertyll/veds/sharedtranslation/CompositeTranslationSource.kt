package com.vertyll.veds.sharedtranslation

/**
 * A [TranslationSource] over several languages.
 *
 * @property fallbackDefaults values the owning services declared, packaged at
 *           build time. Not a per-key fallback — a key missing from both still
 *           renders as the key — but enough to keep e-mails going out with
 *           sensible text when the catalogue service is unreachable at start-up.
 */
class CompositeTranslationSource(
    private val snapshots: () -> Map<String, TranslationSnapshot>,
    private val fallbackDefaults: Map<String, Map<String, String>> = emptyMap(),
) : TranslationSource {
    override fun patternFor(
        key: String,
        language: String,
    ): String? {
        val normalized = language.lowercase()
        snapshots()[normalized]?.entries?.get(key)?.let { return it }
        return fallbackDefaults[normalized]?.get(key)
    }
}
