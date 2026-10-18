package com.vertyll.veds.translation.infrastructure.config

import com.vertyll.veds.sharedtranslation.TranslationCatalogue
import com.vertyll.veds.sharedtranslation.translations
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
internal class TranslationCatalogueConfig {
    @Bean
    fun translationTranslationCatalogue(): TranslationCatalogue =
        translations("translation-service") {
            key("common.version_mismatch") {
                pl("Ktoś zmienił te dane w międzyczasie. Odśwież i spróbuj ponownie.")
                en("Somebody changed this in the meantime. Refresh and try again.")
            }
            key("common.language_not_supplied") {
                pl("Nie podano języka.")
                en("No language was supplied.")
            }
            key("common.language_not_supported") {
                pl("Język {language} nie jest obsługiwany.")
                en("Language {language} is not supported.")
            }
            key("common.access_denied") {
                pl("Nie masz uprawnień do tej operacji.")
                en("You are not allowed to perform this action.")
            }
            key("common.not_authenticated") {
                pl("Nie jesteś zalogowany.")
                en("You are not signed in.")
            }
            key("common.token_claim_missing") {
                pl("Token nie zawiera wymaganych danych.")
                en("The token is missing required claims.")
            }
            key("common.validation_failed") {
                pl("Formularz zawiera błędy.")
                en("The form contains errors.")
            }
            key("common.unexpected_error") {
                pl("Coś poszło nie tak.")
                en("Something went wrong.")
            }
            key("common.invalid_value") {
                pl("Nieprawidłowa wartość.")
                en("Invalid value.")
            }
            key("translation.key.not_found") {
                pl("Nie znaleziono klucza.")
                en("Key not found.")
            }
            key("translation.key.owned_by_another_service") {
                pl("Klucz należy do usługi {owner}.")
                en("This key belongs to {owner}.")
            }
            key("translation.language.not_found") {
                pl("Nie znaleziono języka {language}.")
                en("Language {language} not found.")
            }
            key("translation.value.not_found") {
                pl("Nie znaleziono tłumaczenia.")
                en("Translation not found.")
            }
            key("translation.value.invalid_icu_pattern") {
                pl("Nieprawidłowy wzorzec ICU: {reason}")
                en("Invalid ICU pattern: {reason}")
            }
            key("translation.value.unknown_arguments") {
                pl("Wzorzec musi używać tych samych zmiennych co wartość domyślna: {reason}")
                en("The pattern must use the same arguments as the shipped default: {reason}")
            }
            key("translation.import.malformed") {
                pl("Nie udało się odczytać pliku.")
                en("The file could not be read.")
            }
            key("validation.translation.value_required") {
                pl("Treść tłumaczenia jest wymagana.")
                en("Translation text is required.")
            }
            key("validation.translation.key_required") {
                pl("Klucz jest wymagany.")
                en("Key is required.")
            }
            key("validation.translation.source_service_required") {
                pl("Nazwa usługi jest wymagana.")
                en("Source service is required.")
            }
            key("validation.translation.entries_required") {
                pl("Katalog jest pusty.")
                en("The catalogue is empty.")
            }
            key("validation.translation.default_values_required") {
                pl("Brak wartości domyślnych.")
                en("No default values were supplied.")
            }
            key("translation.export.column.key") {
                pl("Klucz")
                en("Key")
            }
            key("translation.export.column.source_service") {
                pl("Usługa")
                en("Service")
            }
            key("translation.export.column.description") {
                pl("Opis")
                en("Description")
            }
            key("admin.tabs.roles") {
                pl("Role")
                en("Roles")
            }
            key("admin.tabs.rights") {
                pl("Uprawnienia")
                en("Rights")
            }
            key("admin.tabs.translations") {
                pl("Tłumaczenia")
                en("Translations")
            }
            key("admin.roles.title") {
                pl("Role i ich uprawnienia")
                en("Roles and what they grant")
            }
            key("admin.roles.name") {
                pl("Rola")
                en("Role")
            }
            key("admin.roles.description") {
                pl("Opis")
                en("Description")
            }
            key("admin.roles.permissions") {
                pl("Uprawnienia")
                en("Permissions")
            }
            key("admin.roles.no_permissions") {
                pl("Brak uprawnień")
                en("No permissions")
            }
            key("admin.rights.title") {
                pl("Uprawnienia i role, które ich udzielają")
                en("Rights and the roles that grant them")
            }
            key("admin.rights.name") {
                pl("Uprawnienie")
                en("Right")
            }
            key("admin.rights.description") {
                pl("Opis")
                en("Description")
            }
            key("admin.rights.granted_by") {
                pl("Nadawane przez role")
                en("Granted by roles")
            }
            key("admin.rights.granted_by_nobody") {
                pl("Żadna rola tego nie nadaje")
                en("No role grants this")
            }
            key("admin.translations.title") {
                pl("Tłumaczenia")
                en("Translations")
            }
            key("admin.translations.key") {
                pl("Klucz")
                en("Key")
            }
            key("admin.translations.source") {
                pl("Usługa")
                en("Service")
            }
            key("admin.translations.values") {
                pl("Wartości")
                en("Values")
            }
            key("admin.translations.search") {
                pl("Szukaj klucza")
                en("Search keys")
            }
            key("admin.translations.only_missing") {
                pl("Tylko brakujące")
                en("Only missing")
            }
            key("admin.translations.missing") {
                pl("brak")
                en("missing")
            }
            key("admin.translations.revert") {
                pl("Przywróć domyślne")
                en("Revert")
            }
            key("admin.translations.export") {
                pl("Eksport XLSX")
                en("Export XLSX")
            }
            key("admin.translations.import") {
                pl("Import XLSX")
                en("Import XLSX")
            }
            key("admin.translations.import_applied") {
                pl(
                    "{count, plural, one{Zastosowano # wiersz} few{Zastosowano # wiersze} many{Zastosowano # wierszy} other{Zastosowano # wiersza}}",
                )
                en("{count, plural, one{Applied # row} other{Applied # rows}}")
            }
            key("admin.translations.import_unknown_keys") {
                pl(
                    "{count, plural, one{Pominięto # nieznany klucz} few{Pominięto # nieznane klucze} many{Pominięto # nieznanych kluczy} other{Pominięto # nieznanego klucza}}",
                )
                en("{count, plural, one{Skipped # unknown key} other{Skipped # unknown keys}}")
            }
            key("admin.translations.import_rejected") {
                pl(
                    "{count, plural, one{Odrzucono # wzorzec ICU} few{Odrzucono # wzorce ICU} many{Odrzucono # wzorców ICU} other{Odrzucono # wzorca ICU}}",
                )
                en("{count, plural, one{Rejected # ICU pattern} other{Rejected # ICU patterns}}")
            }
            key("security.two_factor.title") {
                pl("Weryfikacja dwuetapowa")
                en("Two-factor authentication")
            }
            key("security.two_factor.description") {
                pl("Dodatkowy kod z aplikacji uwierzytelniającej przy każdym logowaniu.")
                en("An extra code from an authenticator app at every sign-in.")
            }
            key("security.two_factor.enabled") {
                pl("Włączona")
                en("Enabled")
            }
            key("security.two_factor.disabled") {
                pl("Wyłączona")
                en("Disabled")
            }
            key("security.two_factor.enable") {
                pl("Włącz")
                en("Enable")
            }
            key("security.two_factor.disable") {
                pl("Wyłącz")
                en("Disable")
            }
            key("security.two_factor.reconfigure") {
                pl("Skonfiguruj ponownie")
                en("Reconfigure")
            }
            key("security.two_factor.disable_warning") {
                pl("Wyłączenie obniża bezpieczeństwo konta.")
                en("Turning this off makes the account less secure.")
            }
            key("security.two_factor.unavailable") {
                pl("Nie udało się odczytać ustawień bezpieczeństwa.")
                en("Security settings could not be loaded.")
            }
            key("common.loading") {
                pl("Wczytywanie…")
                en("Loading…")
            }
        }
}
