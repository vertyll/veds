package com.vertyll.veds.sharedtranslation

/**
 * Receiver of the [translations] DSL. Not constructed directly.
 */
@TranslationDsl
class TranslationCatalogueBuilder(
    private val sourceService: String,
) {
    private val definitions = mutableListOf<TranslationKeyDefinition>()
    private val declared = mutableSetOf<String>()

    fun key(
        key: String,
        description: String? = null,
        block: TranslationValuesBuilder.() -> Unit,
    ) {
        require(declared.add(key)) { "duplicate translation key '$key' in $sourceService" }

        val values = TranslationValuesBuilder().apply(block).build()
        values.forEach { (language, pattern) ->
            IcuPatternValidator.requireValid(key, language, pattern)
        }
        definitions +=
            TranslationKeyDefinition(
                key = key,
                defaultValues = values,
                description = description,
            )
    }

    internal fun build(): TranslationCatalogue =
        TranslationCatalogue(
            sourceService = sourceService,
            definitions = definitions.toList(),
        )
}

/**
 * Declares the translation keys owned by one service.
 *
 * ```
 * val catalogue = translations("project-service") {
 *     key("project.not_found") {
 *         pl("Nie znaleziono projektu")
 *         en("Project not found")
 *     }
 * }
 * ```
 *
 * Patterns are validated as they are declared, so a malformed ICU pattern fails
 * at start-up in the service that owns it rather than at render time on
 * somebody's screen.
 *
 * @param sourceService identifies the owner; registration refuses a key another
 *        service already declared.
 * @throws IllegalArgumentException on a duplicate key or an invalid pattern.
 */
fun translations(
    sourceService: String,
    block: TranslationCatalogueBuilder.() -> Unit,
): TranslationCatalogue = TranslationCatalogueBuilder(sourceService).apply(block).build()
