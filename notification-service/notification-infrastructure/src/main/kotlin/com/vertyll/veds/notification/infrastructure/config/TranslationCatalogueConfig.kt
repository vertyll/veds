package com.vertyll.veds.notification.infrastructure.config

import com.vertyll.veds.sharedtranslation.TranslationCatalogue
import com.vertyll.veds.sharedtranslation.translations
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
internal class TranslationCatalogueConfig {
    @Bean
    fun notificationTranslationCatalogue(): TranslationCatalogue =
        translations("notification-service") {
            key("notification.not_found") {
                pl("Nie znaleziono powiadomienia.")
                en("Notification not found.")
            }
            key("notification.project_invitation") {
                pl("Zaproszenie do projektu {projectName}.")
                en("You have been invited to {projectName}.")
            }
            key("notification.project_member_joined") {
                pl("Dołączono Cię do projektu jako {roleCode}.")
                en("You joined the project as {roleCode}.")
            }
            key("notification.task_created") {
                pl("Nowe zadanie: {taskName}.")
                en("New task: {taskName}.")
            }
            key("notification.task_assigned") {
                pl("Przypisano Ci zadanie.")
                en("A task was assigned to you.")
            }
            key("notification.task_status_changed") {
                pl("Zmieniono status zadania.")
                en("A task changed status.")
            }
            key("notification.task_comment_added") {
                pl("Nowy komentarz: {excerpt}")
                en("New comment: {excerpt}")
            }
            key("validation.notification.ids_required") {
                pl("Nie wskazano powiadomień.")
                en("No notifications were given.")
            }
        }
}
