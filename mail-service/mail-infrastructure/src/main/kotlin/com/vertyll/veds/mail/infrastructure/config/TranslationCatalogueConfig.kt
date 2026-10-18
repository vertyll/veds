package com.vertyll.veds.mail.infrastructure.config

import com.vertyll.veds.sharedtranslation.TranslationCatalogue
import com.vertyll.veds.sharedtranslation.translations
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
internal class TranslationCatalogueConfig {
    @Bean
    fun mailTranslationCatalogue(): TranslationCatalogue =
        translations("mail-service") {
            key("mail.template.unknown") {
                pl("Nieznana nazwa szablonu wiadomości.")
                en("Unknown message template name.")
            }
            key("validation.mail.recipients_required") {
                pl("Wymagany jest co najmniej jeden odbiorca.")
                en("At least one recipient is required.")
            }
            key("validation.mail.recipient_email_required") {
                pl("Adres odbiorcy jest wymagany.")
                en("The recipient address is required.")
            }
            key("validation.mail.template_name_required") {
                pl("Nazwa szablonu jest wymagana.")
                en("A template name is required.")
            }
        }
}
