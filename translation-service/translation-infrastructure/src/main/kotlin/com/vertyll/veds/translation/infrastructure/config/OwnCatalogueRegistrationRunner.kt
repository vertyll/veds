package com.vertyll.veds.translation.infrastructure.config

import com.vertyll.veds.sharedtranslation.TranslationCatalogue
import com.vertyll.veds.translation.application.command.CatalogueEntryCommand
import com.vertyll.veds.translation.application.command.RegisterCatalogueCommand
import com.vertyll.veds.translation.application.port.inbound.command.TranslationCommandUseCase
import org.slf4j.LoggerFactory
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component

@Component
@Order(2)
internal class OwnCatalogueRegistrationRunner(
    private val catalogues: List<TranslationCatalogue>,
    private val commands: TranslationCommandUseCase,
) : ApplicationRunner {
    private val logger = LoggerFactory.getLogger(javaClass)

    override fun run(args: ApplicationArguments) {
        catalogues.forEach { catalogue ->
            try {
                val applied = commands.registerCatalogue(catalogue.toCommand())
                logger.info("Registered {} own translation keys from {}", applied, catalogue.sourceService)
            } catch (e: Exception) {
                logger.error(
                    "Could not register the own translation catalogue for {}: {}. " +
                        "The service continues; keys will be republished on the next start.",
                    catalogue.sourceService,
                    e.message,
                )
            }
        }
    }

    private fun TranslationCatalogue.toCommand() =
        RegisterCatalogueCommand(
            sourceService = sourceService,
            entries =
                definitions.map {
                    CatalogueEntryCommand(
                        key = it.key,
                        description = it.description,
                        defaultValues = it.defaultValues,
                    )
                },
        )
}
