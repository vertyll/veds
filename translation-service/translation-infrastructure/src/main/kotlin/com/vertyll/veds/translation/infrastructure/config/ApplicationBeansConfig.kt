package com.vertyll.veds.translation.infrastructure.config

import com.vertyll.veds.translation.application.port.inbound.command.TranslationCommandUseCase
import com.vertyll.veds.translation.application.port.inbound.query.TranslationExportUseCase
import com.vertyll.veds.translation.application.port.inbound.query.TranslationQueryUseCase
import com.vertyll.veds.translation.application.service.command.TranslationCommandService
import com.vertyll.veds.translation.application.service.query.TranslationExportService
import com.vertyll.veds.translation.application.service.query.TranslationQueryService
import com.vertyll.veds.translation.domain.repository.LanguageRepository
import com.vertyll.veds.translation.domain.repository.TranslationKeyRepository
import com.vertyll.veds.translation.domain.repository.TranslationValueRepository
import com.vertyll.veds.translation.infrastructure.logging.Slf4jUseCaseLogger
import com.vertyll.veds.translation.infrastructure.transaction.TransactionalUseCaseFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
@Suppress("TooManyFunctions", "LongParameterList")
internal class ApplicationBeansConfig {
    private companion object {
        private val ALL_METHODS: (String) -> Boolean = { true }
        private val NO_METHODS: (String) -> Boolean = { false }
    }

    @Bean
    fun translationCommandUseCase(
        transactions: TransactionalUseCaseFactory,
        keyRepository: TranslationKeyRepository,
        valueRepository: TranslationValueRepository,
        languageRepository: LanguageRepository,
    ): TranslationCommandUseCase =
        transactions.wrap(
            TranslationCommandUseCase::class.java,
            TranslationCommandService(
                keyRepository,
                valueRepository,
                languageRepository,
                Slf4jUseCaseLogger(TranslationCommandService::class.java),
            ),
            NO_METHODS,
        )

    @Bean
    fun translationExportUseCase(
        transactions: TransactionalUseCaseFactory,
        keyRepository: TranslationKeyRepository,
        valueRepository: TranslationValueRepository,
        languageRepository: LanguageRepository,
    ): TranslationExportUseCase =
        transactions.wrap(
            TranslationExportUseCase::class.java,
            TranslationExportService(
                keyRepository,
                valueRepository,
                languageRepository,
            ),
            ALL_METHODS,
        )

    @Bean
    fun translationQueryUseCase(
        transactions: TransactionalUseCaseFactory,
        keyRepository: TranslationKeyRepository,
        valueRepository: TranslationValueRepository,
        languageRepository: LanguageRepository,
    ): TranslationQueryUseCase =
        transactions.wrap(
            TranslationQueryUseCase::class.java,
            TranslationQueryService(
                keyRepository,
                valueRepository,
                languageRepository,
            ),
            ALL_METHODS,
        )
}
