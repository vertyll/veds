package com.vertyll.veds.sharedtranslation

/**
 * One translation key as declared by the service that owns it.
 *
 * @property defaultValues what the owning service ships. Seeded as
 *           `default_value` and never allowed to overwrite an administrator's
 *           edit — the two are separate columns precisely so that a redeployment
 *           cannot undo somebody's correction.
 */
data class TranslationKeyDefinition(
    val key: String,
    val defaultValues: Map<String, String>,
    val description: String? = null,
) {
    init {
        require(key.isNotBlank()) { "translation key must not be blank" }
        require(defaultValues.isNotEmpty()) { "key '$key' declares no default value" }
        defaultValues.forEach { (language, value) ->
            require(language.isNotBlank()) { "key '$key' has a blank language code" }
            require(value.isNotBlank()) { "key '$key' has a blank value for '$language'" }
        }
    }
}
