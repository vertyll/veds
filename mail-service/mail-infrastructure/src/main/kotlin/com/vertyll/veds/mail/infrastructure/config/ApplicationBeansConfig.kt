package com.vertyll.veds.mail.infrastructure.config

import com.vertyll.veds.mail.application.port.inbound.EmailBatchUseCase
import com.vertyll.veds.mail.application.port.inbound.EmailSagaUseCase
import com.vertyll.veds.mail.application.port.inbound.EmailUseCase
import com.vertyll.veds.mail.application.port.outbound.MailFeedbackEventPublisherPort
import com.vertyll.veds.mail.application.port.outbound.MailSenderPort
import com.vertyll.veds.mail.application.port.outbound.TemplateRendererPort
import com.vertyll.veds.mail.application.service.EmailBatchService
import com.vertyll.veds.mail.application.service.EmailSagaService
import com.vertyll.veds.mail.application.service.EmailService
import com.vertyll.veds.mail.domain.model.SenderAddress
import com.vertyll.veds.mail.domain.repository.EmailLogRepository
import com.vertyll.veds.mail.infrastructure.logging.Slf4jUseCaseLogger
import com.vertyll.veds.mail.infrastructure.transaction.TransactionalUseCaseFactory
import com.vertyll.veds.shared.saga.SagaProcessPort
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
@Suppress("TooManyFunctions", "LongParameterList")
internal class ApplicationBeansConfig {
    private companion object {
        private val NO_METHODS: (String) -> Boolean = { false }
    }

    @Bean
    fun emailBatchUseCase(
        transactions: TransactionalUseCaseFactory,
        emailService: EmailUseCase,
    ): EmailBatchUseCase =
        transactions.wrap(
            EmailBatchUseCase::class.java,
            EmailBatchService(
                emailService,
            ),
            NO_METHODS,
        )

    @Bean
    fun emailSagaUseCase(
        transactions: TransactionalUseCaseFactory,
        sagaProcess: SagaProcessPort,
        emailService: EmailUseCase,
        mailFeedbackPublisher: MailFeedbackEventPublisherPort,
    ): EmailSagaUseCase =
        transactions.wrap(
            EmailSagaUseCase::class.java,
            EmailSagaService(
                sagaProcess,
                emailService,
                mailFeedbackPublisher,
                Slf4jUseCaseLogger(EmailSagaService::class.java),
            ),
            NO_METHODS,
        )

    @Bean
    fun emailUseCase(
        transactions: TransactionalUseCaseFactory,
        mailSender: MailSenderPort,
        templateRenderer: TemplateRendererPort,
        emailLogRepository: EmailLogRepository,
        mailProperties: MailProperties,
    ): EmailUseCase =
        transactions.wrap(
            EmailUseCase::class.java,
            EmailService(
                mailSender,
                templateRenderer,
                emailLogRepository,
                SenderAddress(mailProperties.from),
                Slf4jUseCaseLogger(EmailService::class.java),
            ),
        ) { it in setOf("getEmailLogs") }
}
