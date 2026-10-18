package com.vertyll.veds.task.infrastructure.config

import com.vertyll.veds.sharedtranslation.TranslationCatalogue
import com.vertyll.veds.sharedtranslation.translations
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
internal class TranslationCatalogueConfig {
    @Bean
    fun taskTranslationCatalogue(): TranslationCatalogue =
        translations("task-service") {
            key("task.not_found") {
                pl("Nie znaleziono zadania.")
                en("Task not found.")
            }
            key("task.access_denied") {
                pl("Nie masz uprawnień do tego zadania.")
                en("You do not have permission for this task.")
            }
            key("task.comment.not_found") {
                pl("Nie znaleziono komentarza.")
                en("Comment not found.")
            }
            key("task.comment.not_authored_by_caller") {
                pl("Możesz edytować tylko własne komentarze.")
                en("You can only edit your own comments.")
            }
            key("task.work_log.invalid_visibility") {
                pl("Nieznany zakres widoczności dziennika pracy.")
                en("Unknown work log visibility.")
            }
            key("task.work_log.not_found") {
                pl("Nie znaleziono wpisu czasu pracy.")
                en("Work log entry not found.")
            }
            key("task.work_log.not_authored_by_caller") {
                pl("Możesz edytować tylko własne wpisy czasu pracy.")
                en("You can only edit your own work log entries.")
            }
            key("task.project.not_known") {
                pl("Projekt nie jest jeszcze znany tej usłudze.")
                en("This service does not know that project yet.")
            }
            key("task.project.archived") {
                pl("Projekt jest zarchiwizowany.")
                en("The project is archived.")
            }
            key("task.status.not_in_project") {
                pl("Ten status nie należy do projektu.")
                en("That status does not belong to the project.")
            }
            key("task.category.not_in_project") {
                pl("Ta kategoria nie należy do projektu.")
                en("That category does not belong to the project.")
            }
            key("task.assignee.not_a_member") {
                pl("Ta osoba nie jest członkiem projektu.")
                en("That person is not a member of the project.")
            }
            key("validation.task.description_required") {
                pl("Opis zadania jest wymagany.")
                en("Task description is required.")
            }
            key("validation.task.estimation_negative") {
                pl("Wycena nie może być ujemna.")
                en("The estimate cannot be negative.")
            }
            key("validation.task.work_log.minutes_positive") {
                pl("Zalogowany czas musi być większy od zera.")
                en("Logged time must be greater than zero.")
            }
            key("validation.task.work_log.minutes_too_large") {
                pl("Pojedynczy wpis nie może przekraczać 24 godzin.")
                en("A single entry cannot exceed 24 hours.")
            }
            key("validation.task.work_log.worked_on_required") {
                pl("Data wykonania pracy jest wymagana.")
                en("The date the work was done is required.")
            }
            key("validation.task.work_log.description_too_long") {
                pl("Opis wpisu może mieć najwyżej 500 znaków.")
                en("An entry description may be at most 500 characters.")
            }
            key("validation.task.batch_empty") {
                pl("Nie wybrano żadnego zadania.")
                en("No tasks were selected.")
            }
            key("validation.task.comment_content_required") {
                pl("Treść komentarza jest wymagana.")
                en("Comment content is required.")
            }
            key("task.work_logged") {
                pl("Czas pracy zapisany.")
                en("Work logged.")
            }
        }
}
