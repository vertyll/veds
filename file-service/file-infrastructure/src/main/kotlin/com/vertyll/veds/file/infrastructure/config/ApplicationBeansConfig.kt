package com.vertyll.veds.file.infrastructure.config

import com.vertyll.veds.file.application.port.inbound.command.FileCommandUseCase
import com.vertyll.veds.file.application.port.inbound.query.FileQueryUseCase
import com.vertyll.veds.file.application.port.outbound.FileEventPublisherPort
import com.vertyll.veds.file.application.port.outbound.ObjectStoragePort
import com.vertyll.veds.file.application.service.command.FileCommandService
import com.vertyll.veds.file.application.service.query.FileQueryService
import com.vertyll.veds.file.domain.repository.StoredFileRepository
import com.vertyll.veds.file.infrastructure.logging.Slf4jUseCaseLogger
import com.vertyll.veds.file.infrastructure.transaction.TransactionalUseCaseFactory
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
    fun fileCommandUseCase(
        transactions: TransactionalUseCaseFactory,
        fileRepository: StoredFileRepository,
        storage: ObjectStoragePort,
        eventPublisher: FileEventPublisherPort,
    ): FileCommandUseCase =
        transactions.wrap(
            FileCommandUseCase::class.java,
            FileCommandService(
                fileRepository,
                storage,
                eventPublisher,
                Slf4jUseCaseLogger(FileCommandService::class.java),
            ),
            NO_METHODS,
        )

    @Bean
    fun fileQueryUseCase(
        transactions: TransactionalUseCaseFactory,
        fileRepository: StoredFileRepository,
        storage: ObjectStoragePort,
    ): FileQueryUseCase =
        transactions.wrap(
            FileQueryUseCase::class.java,
            FileQueryService(
                fileRepository,
                storage,
            ),
            ALL_METHODS,
        )
}
