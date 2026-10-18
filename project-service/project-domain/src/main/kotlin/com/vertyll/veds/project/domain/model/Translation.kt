package com.vertyll.veds.project.domain.model

data class Translation(
    val language: LanguageTag,
    val name: String,
    val description: String? = null,
) {
    init {
        require(name.isNotBlank()) { "translation name must not be blank" }
    }
}

internal fun requireAtLeastOneTranslation(translations: Set<Translation>) {
    require(translations.isNotEmpty()) { "at least one translation is required" }
    val languages = translations.map { it.language }
    require(languages.size == languages.toSet().size) { "duplicate translations for the same language" }
}

fun Set<Translation>.resolveFor(language: LanguageTag): Translation =
    firstOrNull { it.language == language }
        ?: first()
