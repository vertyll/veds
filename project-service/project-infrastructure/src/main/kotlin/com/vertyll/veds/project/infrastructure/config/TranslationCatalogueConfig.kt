package com.vertyll.veds.project.infrastructure.config

import com.vertyll.veds.sharedtranslation.TranslationCatalogue
import com.vertyll.veds.sharedtranslation.translations
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
internal class TranslationCatalogueConfig {
    @Bean
    fun projectTranslationCatalogue(): TranslationCatalogue =
        translations("project-service") {
            key("project.not_found") {
                pl("Nie znaleziono projektu.")
                en("Project not found.")
            }
            key("project.access_denied") {
                pl("Nie masz uprawnień do tej operacji.")
                en("You do not have permission to do this.")
            }
            key("project.archived") {
                pl("Projekt jest zarchiwizowany i nie można go zmieniać.")
                en("This project is archived and cannot be changed.")
            }
            key("project.type.not_found") {
                pl("Nie znaleziono typu projektu.")
                en("Project type not found.")
            }
            key("project.role.not_found") {
                pl("Nie znaleziono roli projektowej.")
                en("Project role not found.")
            }
            key("project.role.not_configured") {
                pl("Role projektowe nie są skonfigurowane.")
                en("Project roles are not configured.")
            }
            key("project.category.not_found") {
                pl("Nie znaleziono kategorii.")
                en("Category not found.")
            }
            key("project.status.not_found") {
                pl("Nie znaleziono statusu.")
                en("Status not found.")
            }
            key("project.member.not_found") {
                pl("Nie znaleziono członka projektu.")
                en("Project member not found.")
            }
            key("project.member.already_joined") {
                pl("Ta osoba jest już w projekcie.")
                en("This person is already a member.")
            }
            key("project.member.owner_immutable") {
                pl("Nie można zmienić roli właściciela projektu.")
                en("The project owner's role cannot be changed.")
            }
            key("project.invitation.not_found") {
                pl("Nie znaleziono zaproszenia.")
                en("Invitation not found.")
            }
            key("project.invitation.not_pending") {
                pl("To zaproszenie zostało już rozpatrzone.")
                en("This invitation has already been answered.")
            }
            key("project.invitation.expired") {
                pl("Zaproszenie wygasło.")
                en("The invitation has expired.")
            }
            key("project.invitation.already_sent") {
                pl("Ta osoba ma już oczekujące zaproszenie.")
                en("This person already has a pending invitation.")
            }
            key("project.invitation.not_addressed_to_caller") {
                pl("To zaproszenie nie jest skierowane do Ciebie.")
                en("This invitation was not addressed to you.")
            }
            key("project.translation.missing") {
                pl("Brakuje tłumaczeń dla: {missing}.")
                en("Translations are missing for: {missing}.")
            }
            key("validation.project.name_required") {
                pl("Nazwa projektu jest wymagana.")
                en("Project name is required.")
            }
            key("validation.project.name_too_long") {
                pl("Nazwa projektu jest za długa.")
                en("Project name is too long.")
            }
            key("validation.project.description_too_long") {
                pl("Opis jest za długi.")
                en("Description is too long.")
            }
            key("validation.project.color_required") {
                pl("Kolor jest wymagany.")
                en("Colour is required.")
            }
            key("validation.project.translations_required") {
                pl("Wymagane jest co najmniej jedno tłumaczenie.")
                en("At least one translation is required.")
            }
            key("validation.project.translation_name_required") {
                pl("Nazwa tłumaczenia jest wymagana.")
                en("Translation name is required.")
            }
            key("validation.project.invitee_email_required") {
                pl("Adres e-mail jest wymagany.")
                en("E-mail address is required.")
            }
            key("validation.project.invitee_email_invalid") {
                pl("Nieprawidłowy adres e-mail.")
                en("Invalid e-mail address.")
            }
        }
}
