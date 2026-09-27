package com.vertyll.veds.sharedtranslation

/**
 * Where [MessageResolver] reads patterns from: a cache, a snapshot, a test
 * fixture or the build-time defaults.
 */
fun interface TranslationSource {
    fun patternFor(
        key: String,
        language: String,
    ): String?
}
