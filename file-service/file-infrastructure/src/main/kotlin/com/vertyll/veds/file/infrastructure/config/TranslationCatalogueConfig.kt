package com.vertyll.veds.file.infrastructure.config

import com.vertyll.veds.sharedtranslation.TranslationCatalogue
import com.vertyll.veds.sharedtranslation.translations
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
internal class TranslationCatalogueConfig {
    @Bean
    fun fileTranslationCatalogue(): TranslationCatalogue =
        translations("file-service") {
            key("file.not_found") {
                pl("Nie znaleziono pliku.")
                en("File not found.")
            }
            key("file.not_available") {
                pl("Plik nie jest jeszcze dostępny.")
                en("This file is not available yet.")
            }
            key("file.content_type_not_allowed") {
                pl("Ten typ pliku nie jest dozwolony tutaj.")
                en("This file type is not allowed here.")
            }
            key("file.too_large") {
                pl("Plik jest za duży.")
                en("The file is too large.")
            }
            key("file.upload_not_pending") {
                pl("Ten plik został już przesłany.")
                en("This upload was already completed.")
            }
            key("file.object_missing_in_storage") {
                pl("Przesyłanie nie zostało zakończone.")
                en("The upload did not finish.")
            }
            key("validation.file.name_required") {
                pl("Nazwa pliku jest wymagana.")
                en("A file name is required.")
            }
            key("validation.file.content_type_required") {
                pl("Typ pliku jest wymagany.")
                en("A content type is required.")
            }
            key("validation.file.size_positive") {
                pl("Rozmiar pliku musi być dodatni.")
                en("The file size must be positive.")
            }
        }
}
