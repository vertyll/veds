package com.vertyll.projecta.sharedinfrastructure.http

object ETagUtil {
    private const val WEAK_PREFIX = "W/\""
    private const val STRONG_QUOTE = '"'

    // Build a weak ETag from a nullable version
    fun buildWeakETag(version: Long?): String? = version?.let { "$WEAK_PREFIX$it\"" }

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
