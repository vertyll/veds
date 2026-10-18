package com.vertyll.veds.template.infrastructure.config

import com.vertyll.veds.shared.saga.SagaProcessPort
import com.vertyll.veds.template.application.port.inbound.TemplateCompensationUseCase
import com.vertyll.veds.template.application.port.inbound.command.TemplateCommandUseCase
import com.vertyll.veds.template.application.port.inbound.query.TemplateQueryUseCase
import com.vertyll.veds.template.application.service.TemplateCompensationService
import com.vertyll.veds.template.application.service.command.TemplateCommandService
import com.vertyll.veds.template.application.service.query.TemplateQueryService
import com.vertyll.veds.template.domain.repository.TemplateRepository
import com.vertyll.veds.template.infrastructure.logging.Slf4jUseCaseLogger
import com.vertyll.veds.template.infrastructure.transaction.TransactionalUseCaseFactory
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
    fun templateCompensationUseCase(
        transactions: TransactionalUseCaseFactory,
        templateRepository: TemplateRepository,
    ): TemplateCompensationUseCase =
        transactions.wrap(
            TemplateCompensationUseCase::class.java,
            TemplateCompensationService(
                templateRepository,
                Slf4jUseCaseLogger(TemplateCompensationService::class.java),
            ),
            NO_METHODS,
        )

    @Bean
    fun templateCommandUseCase(
        transactions: TransactionalUseCaseFactory,
        sagaProcess: SagaProcessPort,
        templateRepository: TemplateRepository,
    ): TemplateCommandUseCase =
        transactions.wrap(
            TemplateCommandUseCase::class.java,
            TemplateCommandService(
                sagaProcess,
                templateRepository,
                Slf4jUseCaseLogger(TemplateCommandService::class.java),
            ),
            NO_METHODS,
        )

    @Bean
    fun templateQueryUseCase(
        transactions: TransactionalUseCaseFactory,
        templateRepository: TemplateRepository,
    ): TemplateQueryUseCase =
        transactions.wrap(
            TemplateQueryUseCase::class.java,
            TemplateQueryService(
                templateRepository,
            ),
            ALL_METHODS,
        )
}
