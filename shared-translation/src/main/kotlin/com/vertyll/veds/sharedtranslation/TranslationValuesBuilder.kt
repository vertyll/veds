package com.vertyll.veds.sharedtranslation

/**
 * Collects one key's text per language. [pl] and [en] are conveniences over
 * [TranslationValuesBuilder.language].
 */
@TranslationDsl
class TranslationValuesBuilder {
    private val values = linkedMapOf<String, String>()

    fun pl(value: String) = language("pl", value)

    fun en(value: String) = language("en", value)

    fun language(
        code: String,
        value: String,
    ) {
        require(values.put(code.lowercase(), value) == null) {
            "language '$code' declared twice for the same key"
        }
    }

    internal fun build(): Map<String, String> = values.toMap()
}
