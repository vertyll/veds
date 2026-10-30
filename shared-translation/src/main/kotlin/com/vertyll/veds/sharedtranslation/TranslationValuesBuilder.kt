package com.vertyll.veds.sharedtranslation

/**
 * Collects one key's text per language. [pl] and [en] are conveniences over
 * [TranslationValuesBuilder.language].
 */
@TranslationDsl
class TranslationValuesBuilder {
    private val values = linkedMapOf<String, String>()

    /** Sets the Polish text of the key. */
    fun pl(value: String) {
        language("pl", value)
    }

    /** Sets the English text of the key. */
    fun en(value: String) {
        language("en", value)
    }

    /** Sets the text of the key in the language [code]; a language may be set once. */
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
