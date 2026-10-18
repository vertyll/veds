package com.vertyll.veds.project.domain.model

@JvmInline
value class LanguageTag(
    val value: String,
) {
    init {
        require(value.isNotBlank()) { "language tag must not be blank" }
        require(PATTERN.matches(value)) { "'$value' is not a valid BCP 47 language tag" }
        require(value == value.lowercase()) { "language tag must be lower case, was '$value'" }
    }

    override fun toString(): String = value

    companion object {
        private val PATTERN = Regex("^[a-z]{2,3}(-[a-z0-9]{2,8})*$")

        fun of(raw: String): LanguageTag = LanguageTag(raw.trim().lowercase())

        fun parse(raw: String?): LanguageTag? {
            val candidate = raw?.substringBefore(',')?.trim()?.lowercase() ?: return null
            return if (PATTERN.matches(candidate)) LanguageTag(candidate) else null
        }
    }
}
