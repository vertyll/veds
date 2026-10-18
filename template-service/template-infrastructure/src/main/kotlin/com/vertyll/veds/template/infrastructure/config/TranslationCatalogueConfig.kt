package com.vertyll.veds.template.infrastructure.config

import com.vertyll.veds.sharedtranslation.TranslationCatalogue
import com.vertyll.veds.sharedtranslation.translations
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
internal class TranslationCatalogueConfig {
    @Bean
    fun templateTranslationCatalogue(): TranslationCatalogue =
        translations("template-service") {
            key("template.not_found") {
                pl("Nie znaleziono zasobu.")
                en("Resource not found.")
            }
        }
}
