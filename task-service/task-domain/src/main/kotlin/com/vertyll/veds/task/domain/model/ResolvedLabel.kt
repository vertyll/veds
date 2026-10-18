package com.vertyll.veds.task.domain.model

data class ResolvedLabel(
    val name: String,
    val language: String,
)

internal fun Map<String, String>.resolveLabel(language: LanguageTag): ResolvedLabel {
    this[language.value]?.let { return ResolvedLabel(it, language.value) }
    val entry = entries.minByOrNull { it.key } ?: error("label projection has no names at all")
    return ResolvedLabel(entry.value, entry.key)
}
