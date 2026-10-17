package com.vertyll.veds.iam.infrastructure.config

import com.vertyll.veds.sharedtranslation.TranslationCatalogue
import com.vertyll.veds.sharedtranslation.translations
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
internal class TranslationCatalogueConfig {
    @Bean
    fun iamTranslationCatalogue(): TranslationCatalogue =
        translations("iam-service") {
            key("iam.user.not_found") {
                pl("Nie znaleziono użytkownika.")
                en("User not found.")
            }
            key("iam.user.version_mismatch") {
                pl("Ktoś zmienił te dane w międzyczasie. Odśwież i spróbuj ponownie.")
                en("Somebody changed this in the meantime. Refresh and try again.")
            }
            key("iam.role.not_found") {
                pl("Nie znaleziono roli.")
                en("Role not found.")
            }
            key("iam.role.already_exists") {
                pl("Rola o tej nazwie już istnieje.")
                en("A role with this name already exists.")
            }
            key("iam.role.is_system") {
                pl("Tej roli nie można usunąć — należy do platformy.")
                en("This role belongs to the platform and cannot be deleted.")
            }
            key("iam.role.still_assigned") {
                pl("Rola jest nadal przypisana użytkownikom.")
                en("The role is still assigned to users.")
            }
            key("iam.permission.out_of_scope") {
                pl("Tego uprawnienia nie można nadać roli o tym zakresie.")
                en("This permission cannot be held by a role in that scope.")
            }
            key("iam.role.last_unrestricted") {
                pl("To ostatnia rola z pełnymi uprawnieniami — nie można jej odebrać ani usunąć.")
                en("This is the last role holding every permission; it cannot be removed.")
            }
            key("iam.permission.not_found") {
                pl("Nie znaleziono uprawnienia.")
                en("Permission not found.")
            }
            key("iam.role.default_not_configured") {
                pl("Rola domyślna nie jest skonfigurowana.")
                en("The default role is not configured.")
            }
            key("validation.iam.email_required") {
                pl("Adres e-mail jest wymagany.")
                en("E-mail address is required.")
            }
            key("validation.iam.email_invalid") {
                pl("Nieprawidłowy adres e-mail.")
                en("Invalid e-mail address.")
            }
            key("validation.iam.email_too_long") {
                pl("Adres e-mail jest za długi.")
                en("The e-mail address is too long.")
            }
            key("validation.iam.new_email_required") {
                pl("Nowy adres e-mail jest wymagany.")
                en("The new e-mail address is required.")
            }
            key("validation.iam.new_email_invalid") {
                pl("Nowy adres e-mail jest nieprawidłowy.")
                en("The new e-mail address is invalid.")
            }
            key("validation.iam.password_required") {
                pl("Hasło jest wymagane.")
                en("Password is required.")
            }
            key("validation.iam.new_password_required") {
                pl("Nowe hasło jest wymagane.")
                en("A new password is required.")
            }
            key("validation.iam.current_password_required") {
                pl("Aktualne hasło jest wymagane.")
                en("Your current password is required.")
            }
            key("validation.iam.password_length") {
                pl("Hasło musi mieć od 8 do 128 znaków.")
                en("The password must be 8 to 128 characters long.")
            }
            key("validation.iam.password_complexity") {
                pl("Hasło musi zawierać wielką literę, małą literę, cyfrę i znak specjalny.")
                en("The password must contain an upper-case letter, a lower-case letter, a digit and a special character.")
            }
            key("validation.iam.confirmation_code_required") {
                pl("Kod potwierdzający jest wymagany.")
                en("The confirmation code is required.")
            }
            key("validation.iam.first_name_required") {
                pl("Imię jest wymagane.")
                en("First name is required.")
            }
            key("validation.iam.first_name_length") {
                pl("Imię musi mieć od 2 do 50 znaków.")
                en("First name must be 2 to 50 characters long.")
            }
            key("validation.iam.language_invalid") {
                pl("Nieprawidłowy znacznik języka.")
                en("Invalid language tag.")
            }
            key("validation.iam.last_name_required") {
                pl("Nazwisko jest wymagane.")
                en("Last name is required.")
            }
            key("validation.iam.last_name_length") {
                pl("Nazwisko musi mieć od 2 do 50 znaków.")
                en("Last name must be 2 to 50 characters long.")
            }
            key("iam.user.already_exists") {
                pl("Konto o tym adresie już istnieje.")
                en("An account with this address already exists.")
            }
            key("iam.identity_provider.failed") {
                pl("Nie udało się utworzyć konta.")
                en("The account could not be created.")
            }
            key("iam.role.user.description") {
                pl("Standardowy użytkownik aplikacji")
                en("Standard application user")
            }
            key("iam.role.admin.description") {
                pl("Administrator aplikacji")
                en("Application administrator")
            }
            key("iam.role.shipped.description") {
                pl("Rola dostarczona z platformą")
                en("Ships with the platform")
            }
        }
}
