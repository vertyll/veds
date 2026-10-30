package com.vertyll.veds.sharedtranslation

import com.ibm.icu.text.MessageFormat
import java.util.Locale

/**
 * Checks that a pattern is a compilable ICU message.
 *
 * Run twice for the same reason: an unbalanced brace in
 * `{count, plural, one{…}` does not fail quietly — it throws when the message is
 * rendered, in the middle of a page nobody was editing. So patterns are checked
 * when a service declares them and again when an administrator saves one.
 */
object IcuPatternValidator {
    /** Throws [IllegalArgumentException] when [pattern] of [key] in [language] is not a valid ICU pattern. */
    fun requireValid(
        key: String,
        language: String,
        pattern: String,
    ) {
        val failure = validate(language, pattern)
        require(failure == null) { "invalid ICU pattern for '$key' [$language]: $failure" }
    }

    /** Why [pattern] is not a valid ICU pattern in [language], or `null` when it is. */
    fun validate(
        language: String,
        pattern: String,
    ): String? =
        try {
            MessageFormat(pattern, Locale.forLanguageTag(language))
            null
        } catch (e: IllegalArgumentException) {
            // The message is the whole point: it is shown to whoever tried to save
            // the pattern, so it is returned rather than logged and discarded.
            e.message ?: e.toString()
        }

    /** The names of the arguments [pattern] uses in [language]. */
    fun argumentsOf(
        language: String,
        pattern: String,
    ): Set<String> =
        try {
            MessageFormat(pattern, Locale.forLanguageTag(language)).argumentNames.toSet()
        } catch (ignored: IllegalArgumentException) {
            // A pattern that does not compile has no arguments to report, and the
            // reason is surfaced by `validate` on the same path.
            emptySet()
        }
}
