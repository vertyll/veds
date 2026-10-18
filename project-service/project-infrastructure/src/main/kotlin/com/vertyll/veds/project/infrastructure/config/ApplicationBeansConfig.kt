package com.vertyll.veds.project.infrastructure.config

import com.vertyll.veds.project.application.port.inbound.MailFeedbackUseCase
import com.vertyll.veds.project.application.port.inbound.ProjectCompensationUseCase
import com.vertyll.veds.project.application.port.inbound.command.ProjectCategoryCommandUseCase
import com.vertyll.veds.project.application.port.inbound.command.ProjectCommandUseCase
import com.vertyll.veds.project.application.port.inbound.command.ProjectInvitationCommandUseCase
import com.vertyll.veds.project.application.port.inbound.command.ProjectMembershipCommandUseCase
import com.vertyll.veds.project.application.port.inbound.command.ProjectStatusCommandUseCase
import com.vertyll.veds.project.application.port.inbound.query.ProjectCategoryQueryUseCase
import com.vertyll.veds.project.application.port.inbound.query.ProjectInvitationQueryUseCase
import com.vertyll.veds.project.application.port.inbound.query.ProjectMembershipQueryUseCase
import com.vertyll.veds.project.application.port.inbound.query.ProjectQueryUseCase
import com.vertyll.veds.project.application.port.inbound.query.ProjectRoleQueryUseCase
import com.vertyll.veds.project.application.port.inbound.query.ProjectStatusQueryUseCase
import com.vertyll.veds.project.application.port.inbound.query.ProjectTypeQueryUseCase
import com.vertyll.veds.project.application.port.outbound.ProjectEventPublisherPort
import com.vertyll.veds.project.application.port.outbound.ProjectQueryPort
import com.vertyll.veds.project.application.port.outbound.SupportedLanguagesPort
import com.vertyll.veds.project.application.service.MailFeedbackService
import com.vertyll.veds.project.application.service.MemberViewAssembler
import com.vertyll.veds.project.application.service.ProjectAuthorizationService
import com.vertyll.veds.project.application.service.ProjectCompensationService
import com.vertyll.veds.project.application.service.TranslationCompletenessValidator
import com.vertyll.veds.project.application.service.command.ProjectCategoryCommandService
import com.vertyll.veds.project.application.service.command.ProjectCommandService
import com.vertyll.veds.project.application.service.command.ProjectInvitationCommandService
import com.vertyll.veds.project.application.service.command.ProjectMembershipCommandService
import com.vertyll.veds.project.application.service.command.ProjectStatusCommandService
import com.vertyll.veds.project.application.service.query.ProjectCategoryQueryService
import com.vertyll.veds.project.application.service.query.ProjectInvitationQueryService
import com.vertyll.veds.project.application.service.query.ProjectMembershipQueryService
import com.vertyll.veds.project.application.service.query.ProjectQueryService
import com.vertyll.veds.project.application.service.query.ProjectRoleQueryService
import com.vertyll.veds.project.application.service.query.ProjectStatusQueryService
import com.vertyll.veds.project.application.service.query.ProjectTypeQueryService
import com.vertyll.veds.project.domain.repository.ProjectCategoryRepository
import com.vertyll.veds.project.domain.repository.ProjectInvitationRepository
import com.vertyll.veds.project.domain.repository.ProjectMemberRepository
import com.vertyll.veds.project.domain.repository.ProjectRepository
import com.vertyll.veds.project.domain.repository.ProjectRoleRepository
import com.vertyll.veds.project.domain.repository.ProjectStatusRepository
import com.vertyll.veds.project.domain.repository.ProjectTypeRepository
import com.vertyll.veds.project.domain.repository.UserDirectoryRepository
import com.vertyll.veds.project.infrastructure.logging.Slf4jUseCaseLogger
import com.vertyll.veds.project.infrastructure.transaction.TransactionalUseCaseFactory
import com.vertyll.veds.shared.saga.SagaProcessPort
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
@Suppress("TooManyFunctions", "LongParameterList", "kotlin:S107")
internal class ApplicationBeansConfig {
    private companion object {
        private val ALL_METHODS: (String) -> Boolean = { true }
        private val NO_METHODS: (String) -> Boolean = { false }
    }

    @Bean
    fun mailFeedbackUseCase(
        transactions: TransactionalUseCaseFactory,
        sagaProcessPort: SagaProcessPort,
    ): MailFeedbackUseCase =
        transactions.wrap(
            MailFeedbackUseCase::class.java,
            MailFeedbackService(
                sagaProcessPort,
                Slf4jUseCaseLogger(MailFeedbackService::class.java),
            ),
            NO_METHODS,
        )

    @Bean
    fun projectCompensationUseCase(
        transactions: TransactionalUseCaseFactory,
        invitationRepository: ProjectInvitationRepository,
        projectRepository: ProjectRepository,
    ): ProjectCompensationUseCase =
        transactions.wrap(
            ProjectCompensationUseCase::class.java,
            ProjectCompensationService(
                invitationRepository,
                projectRepository,
                Slf4jUseCaseLogger(ProjectCompensationService::class.java),
            ),
            NO_METHODS,
        )

    @Bean
    fun projectCategoryCommandUseCase(
        transactions: TransactionalUseCaseFactory,
        categoryRepository: ProjectCategoryRepository,
        authorization: ProjectAuthorizationService,
        eventPublisher: ProjectEventPublisherPort,
        translationCompleteness: TranslationCompletenessValidator,
    ): ProjectCategoryCommandUseCase =
        transactions.wrap(
            ProjectCategoryCommandUseCase::class.java,
            ProjectCategoryCommandService(
                categoryRepository,
                authorization,
                eventPublisher,
                translationCompleteness,
            ),
            NO_METHODS,
        )

    @Bean
    fun projectCommandUseCase(
        transactions: TransactionalUseCaseFactory,
        projectRepository: ProjectRepository,
        memberRepository: ProjectMemberRepository,
        roleRepository: ProjectRoleRepository,
        typeRepository: ProjectTypeRepository,
        userDirectory: UserDirectoryRepository,
        authorization: ProjectAuthorizationService,
        eventPublisher: ProjectEventPublisherPort,
    ): ProjectCommandUseCase =
        transactions.wrap(
            ProjectCommandUseCase::class.java,
            ProjectCommandService(
                projectRepository,
                memberRepository,
                roleRepository,
                typeRepository,
                userDirectory,
                authorization,
                eventPublisher,
            ),
            NO_METHODS,
        )

    @Bean
    fun projectInvitationCommandUseCase(
        transactions: TransactionalUseCaseFactory,
        invitationRepository: ProjectInvitationRepository,
        memberRepository: ProjectMemberRepository,
        roleRepository: ProjectRoleRepository,
        projectRepository: ProjectRepository,
        userDirectory: UserDirectoryRepository,
        authorization: ProjectAuthorizationService,
        eventPublisher: ProjectEventPublisherPort,
        sagaProcess: SagaProcessPort,
    ): ProjectInvitationCommandUseCase =
        transactions.wrap(
            ProjectInvitationCommandUseCase::class.java,
            ProjectInvitationCommandService(
                invitationRepository,
                memberRepository,
                roleRepository,
                projectRepository,
                userDirectory,
                authorization,
                eventPublisher,
                sagaProcess,
                Slf4jUseCaseLogger(ProjectInvitationCommandService::class.java),
            ),
            NO_METHODS,
        )

    @Bean
    fun projectMembershipCommandUseCase(
        transactions: TransactionalUseCaseFactory,
        memberRepository: ProjectMemberRepository,
        roleRepository: ProjectRoleRepository,
        memberViewAssembler: MemberViewAssembler,
        authorization: ProjectAuthorizationService,
        eventPublisher: ProjectEventPublisherPort,
    ): ProjectMembershipCommandUseCase =
        transactions.wrap(
            ProjectMembershipCommandUseCase::class.java,
            ProjectMembershipCommandService(
                memberRepository,
                roleRepository,
                memberViewAssembler,
                authorization,
                eventPublisher,
            ),
            NO_METHODS,
        )

    @Bean
    fun projectStatusCommandUseCase(
        transactions: TransactionalUseCaseFactory,
        statusRepository: ProjectStatusRepository,
        authorization: ProjectAuthorizationService,
        eventPublisher: ProjectEventPublisherPort,
        translationCompleteness: TranslationCompletenessValidator,
    ): ProjectStatusCommandUseCase =
        transactions.wrap(
            ProjectStatusCommandUseCase::class.java,
            ProjectStatusCommandService(
                statusRepository,
                authorization,
                eventPublisher,
                translationCompleteness,
            ),
            NO_METHODS,
        )

    @Bean
    fun projectCategoryQueryUseCase(
        transactions: TransactionalUseCaseFactory,
        queryPort: ProjectQueryPort,
        authorization: ProjectAuthorizationService,
    ): ProjectCategoryQueryUseCase =
        transactions.wrap(
            ProjectCategoryQueryUseCase::class.java,
            ProjectCategoryQueryService(
                queryPort,
                authorization,
            ),
            ALL_METHODS,
        )

    @Bean
    fun projectInvitationQueryUseCase(
        transactions: TransactionalUseCaseFactory,
        invitationRepository: ProjectInvitationRepository,
        projectRepository: ProjectRepository,
    ): ProjectInvitationQueryUseCase =
        transactions.wrap(
            ProjectInvitationQueryUseCase::class.java,
            ProjectInvitationQueryService(
                invitationRepository,
                projectRepository,
            ),
            ALL_METHODS,
        )

    @Bean
    fun projectMembershipQueryUseCase(
        transactions: TransactionalUseCaseFactory,
        queryPort: ProjectQueryPort,
        authorization: ProjectAuthorizationService,
    ): ProjectMembershipQueryUseCase =
        transactions.wrap(
            ProjectMembershipQueryUseCase::class.java,
            ProjectMembershipQueryService(
                queryPort,
                authorization,
            ),
            ALL_METHODS,
        )

    @Bean
    fun projectQueryUseCase(
        transactions: TransactionalUseCaseFactory,
        queryPort: ProjectQueryPort,
        typeRepository: ProjectTypeRepository,
        authorization: ProjectAuthorizationService,
    ): ProjectQueryUseCase =
        transactions.wrap(
            ProjectQueryUseCase::class.java,
            ProjectQueryService(
                queryPort,
                typeRepository,
                authorization,
            ),
            ALL_METHODS,
        )

    @Bean
    fun projectRoleQueryUseCase(
        transactions: TransactionalUseCaseFactory,
        roleRepository: ProjectRoleRepository,
    ): ProjectRoleQueryUseCase =
        transactions.wrap(
            ProjectRoleQueryUseCase::class.java,
            ProjectRoleQueryService(
                roleRepository,
            ),
            ALL_METHODS,
        )

    @Bean
    fun projectStatusQueryUseCase(
        transactions: TransactionalUseCaseFactory,
        queryPort: ProjectQueryPort,
        authorization: ProjectAuthorizationService,
    ): ProjectStatusQueryUseCase =
        transactions.wrap(
            ProjectStatusQueryUseCase::class.java,
            ProjectStatusQueryService(
                queryPort,
                authorization,
            ),
            ALL_METHODS,
        )

    @Bean
    fun projectTypeQueryUseCase(
        transactions: TransactionalUseCaseFactory,
        typeRepository: ProjectTypeRepository,
    ): ProjectTypeQueryUseCase =
        transactions.wrap(
            ProjectTypeQueryUseCase::class.java,
            ProjectTypeQueryService(
                typeRepository,
            ),
            ALL_METHODS,
        )

    @Bean
    fun projectAuthorizationService(
        projectRepository: ProjectRepository,
        memberRepository: ProjectMemberRepository,
        roleRepository: ProjectRoleRepository,
    ): ProjectAuthorizationService = ProjectAuthorizationService(projectRepository, memberRepository, roleRepository)

    @Bean
    fun memberViewAssembler(
        roleRepository: ProjectRoleRepository,
        userDirectory: UserDirectoryRepository,
    ): MemberViewAssembler = MemberViewAssembler(roleRepository, userDirectory)

    @Bean
    fun translationCompletenessValidator(supportedLanguages: SupportedLanguagesPort): TranslationCompletenessValidator =
        TranslationCompletenessValidator(supportedLanguages)
}
