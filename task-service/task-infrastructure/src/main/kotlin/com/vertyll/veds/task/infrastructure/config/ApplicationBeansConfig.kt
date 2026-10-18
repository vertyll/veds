package com.vertyll.veds.task.infrastructure.config

import com.vertyll.veds.task.application.port.inbound.FileProjectionUseCase
import com.vertyll.veds.task.application.port.inbound.ProjectProjectionUseCase
import com.vertyll.veds.task.application.port.inbound.command.TaskCommandUseCase
import com.vertyll.veds.task.application.port.inbound.command.TaskCommentCommandUseCase
import com.vertyll.veds.task.application.port.inbound.command.WorkLogCommandUseCase
import com.vertyll.veds.task.application.port.inbound.query.TaskCommentQueryUseCase
import com.vertyll.veds.task.application.port.inbound.query.TaskQueryUseCase
import com.vertyll.veds.task.application.port.inbound.query.WorkLogQueryUseCase
import com.vertyll.veds.task.application.port.outbound.TaskEventPublisherPort
import com.vertyll.veds.task.application.port.outbound.TaskQueryPort
import com.vertyll.veds.task.application.service.FileProjectionService
import com.vertyll.veds.task.application.service.ProjectProjectionService
import com.vertyll.veds.task.application.service.TaskAuthorizationService
import com.vertyll.veds.task.application.service.TaskReferenceValidator
import com.vertyll.veds.task.application.service.command.TaskCommandService
import com.vertyll.veds.task.application.service.command.TaskCommentCommandService
import com.vertyll.veds.task.application.service.command.WorkLogCommandService
import com.vertyll.veds.task.application.service.query.TaskCommentQueryService
import com.vertyll.veds.task.application.service.query.TaskQueryService
import com.vertyll.veds.task.application.service.query.WorkLogQueryService
import com.vertyll.veds.task.domain.repository.ProjectDirectoryRepository
import com.vertyll.veds.task.domain.repository.RolePermissionsRepository
import com.vertyll.veds.task.domain.repository.TaskCommentRepository
import com.vertyll.veds.task.domain.repository.TaskRepository
import com.vertyll.veds.task.domain.repository.UserDirectoryRepository
import com.vertyll.veds.task.domain.repository.WorkLogEntryRepository
import com.vertyll.veds.task.infrastructure.logging.Slf4jUseCaseLogger
import com.vertyll.veds.task.infrastructure.transaction.TransactionalUseCaseFactory
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
    fun fileProjectionUseCase(
        transactions: TransactionalUseCaseFactory,
        taskRepository: TaskRepository,
        commentRepository: TaskCommentRepository,
    ): FileProjectionUseCase =
        transactions.wrap(
            FileProjectionUseCase::class.java,
            FileProjectionService(
                taskRepository,
                commentRepository,
                Slf4jUseCaseLogger(FileProjectionService::class.java),
            ),
            NO_METHODS,
        )

    @Bean
    fun projectProjectionUseCase(
        transactions: TransactionalUseCaseFactory,
        projectDirectory: ProjectDirectoryRepository,
        taskRepository: TaskRepository,
    ): ProjectProjectionUseCase =
        transactions.wrap(
            ProjectProjectionUseCase::class.java,
            ProjectProjectionService(
                projectDirectory,
                taskRepository,
                Slf4jUseCaseLogger(ProjectProjectionService::class.java),
            ),
            NO_METHODS,
        )

    @Bean
    fun taskReferenceValidator(projectDirectory: ProjectDirectoryRepository): TaskReferenceValidator =
        TaskReferenceValidator(projectDirectory)

    @Bean
    fun taskAuthorizationService(
        projectDirectory: ProjectDirectoryRepository,
        taskRepository: TaskRepository,
        rolePermissions: RolePermissionsRepository,
    ): TaskAuthorizationService = TaskAuthorizationService(projectDirectory, taskRepository, rolePermissions)

    @Bean
    fun taskCommandUseCase(
        transactions: TransactionalUseCaseFactory,
        taskRepository: TaskRepository,
        commentRepository: TaskCommentRepository,
        userDirectory: UserDirectoryRepository,
        authorization: TaskAuthorizationService,
        references: TaskReferenceValidator,
        eventPublisher: TaskEventPublisherPort,
    ): TaskCommandUseCase =
        transactions.wrap(
            TaskCommandUseCase::class.java,
            TaskCommandService(
                taskRepository,
                commentRepository,
                userDirectory,
                authorization,
                references,
                eventPublisher,
            ),
            NO_METHODS,
        )

    @Bean
    fun taskCommentCommandUseCase(
        transactions: TransactionalUseCaseFactory,
        commentRepository: TaskCommentRepository,
        userDirectory: UserDirectoryRepository,
        authorization: TaskAuthorizationService,
        eventPublisher: TaskEventPublisherPort,
    ): TaskCommentCommandUseCase =
        transactions.wrap(
            TaskCommentCommandUseCase::class.java,
            TaskCommentCommandService(
                commentRepository,
                userDirectory,
                authorization,
                eventPublisher,
            ),
            NO_METHODS,
        )

    @Bean
    fun workLogCommandUseCase(
        transactions: TransactionalUseCaseFactory,
        entryRepository: WorkLogEntryRepository,
        taskRepository: TaskRepository,
        userDirectory: UserDirectoryRepository,
        projectDirectory: ProjectDirectoryRepository,
        authorization: TaskAuthorizationService,
    ): WorkLogCommandUseCase =
        transactions.wrap(
            WorkLogCommandUseCase::class.java,
            WorkLogCommandService(
                entryRepository,
                taskRepository,
                userDirectory,
                projectDirectory,
                authorization,
            ),
            NO_METHODS,
        )

    @Bean
    fun workLogQueryUseCase(
        transactions: TransactionalUseCaseFactory,
        queryPort: TaskQueryPort,
        authorization: TaskAuthorizationService,
    ): WorkLogQueryUseCase =
        transactions.wrap(
            WorkLogQueryUseCase::class.java,
            WorkLogQueryService(
                queryPort,
                authorization,
            ),
            ALL_METHODS,
        )

    @Bean
    fun taskCommentQueryUseCase(
        transactions: TransactionalUseCaseFactory,
        queryPort: TaskQueryPort,
        authorization: TaskAuthorizationService,
    ): TaskCommentQueryUseCase =
        transactions.wrap(
            TaskCommentQueryUseCase::class.java,
            TaskCommentQueryService(
                queryPort,
                authorization,
            ),
            ALL_METHODS,
        )

    @Bean
    fun taskQueryUseCase(
        transactions: TransactionalUseCaseFactory,
        queryPort: TaskQueryPort,
        projectDirectory: ProjectDirectoryRepository,
        userDirectory: UserDirectoryRepository,
        authorization: TaskAuthorizationService,
    ): TaskQueryUseCase =
        transactions.wrap(
            TaskQueryUseCase::class.java,
            TaskQueryService(
                queryPort,
                projectDirectory,
                userDirectory,
                authorization,
            ),
            ALL_METHODS,
        )
}
