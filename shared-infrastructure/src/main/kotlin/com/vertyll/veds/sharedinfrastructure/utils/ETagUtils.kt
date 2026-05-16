package com.vertyll.veds.sharedinfrastructure.utils

/**
 * Helpers for HTTP `ETag` / `If-Match` handling in support of optimistic
 * concurrency on REST endpoints.
 *
 * The convention is: the JPA `@Version` column of an aggregate is exposed
 * as a **weak** ETag (`W/"<version>"`); clients echo the value back on
 * mutating requests via `If-Match`, and the adapter feeds the parsed
 * version into [OptimisticLockingValidatorUtils.validate] before applying
 * the change.
 */
object ETagUtils {
    private const val WEAK_PREFIX = "W/\""
    private const val STRONG_QUOTE = '"'

    /**
     * Builds a weak ETag of the form `W/"<version>"` for the given
     * nullable JPA `@Version` value. Returns `null` when [version] is
     * `null` so callers can transparently propagate "no version" semantics.
     */
    fun buildWeakETag(version: Long?): String? = version?.let { "$WEAK_PREFIX$it\"" }

    /**
     * Parses an `If-Match` header value back into a `Long` version,
     * tolerating both weak (`W/"42"`) and strong (`"42"`) forms as well as
     * bare numeric values. Returns `null` when the header is missing,
     * blank, or not parseable.
     */
    fun parseIfMatchToVersion(ifMatch: String?): Long? {
        if (ifMatch.isNullOrBlank()) return null
        val trimmed = ifMatch.trim()
        val raw =
            if (trimmed.startsWith("W/\"") && trimmed.endsWith('"')) {
                trimmed.substring(2)
            } else {
                trimmed
            }

        val unquoted = stripQuotes(raw.trim())
        return unquoted.toLongOrNull()
    }

    private fun stripQuotes(str: String): String {
        if (str.length >= 2 &&
            str.startsWith(STRONG_QUOTE) &&
            str.endsWith(STRONG_QUOTE)
        ) {
            return str.substring(1, str.length - 1)
        }
        return str
    }
}
