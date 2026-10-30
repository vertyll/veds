package com.vertyll.veds.sharedtranslation

/**
 * Where [MessageResolver] reads patterns from: a cache, a snapshot, a test
 * fixture or the build-time defaults.
 */
fun interface TranslationSource {
    /** The ICU pattern of [key] in [language], if there is one. */
    fun patternFor(
        key: String,
        language: String,
    ): String?
}
