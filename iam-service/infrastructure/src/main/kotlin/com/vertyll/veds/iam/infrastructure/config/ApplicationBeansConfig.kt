package com.vertyll.veds.iam.infrastructure.config

import com.vertyll.veds.iam.application.port.inbound.command.PermissionCatalogueUseCase
import com.vertyll.veds.iam.application.port.inbound.command.ProvisionCurrentUserUseCase
import com.vertyll.veds.iam.application.port.inbound.command.RoleCommandUseCase
import com.vertyll.veds.iam.application.port.inbound.command.SecurityCommandUseCase
import com.vertyll.veds.iam.application.port.inbound.command.UserCommandUseCase
import com.vertyll.veds.iam.application.port.inbound.query.AuthQueryUseCase
import com.vertyll.veds.iam.application.port.inbound.query.PermissionQueryUseCase
import com.vertyll.veds.iam.application.port.inbound.query.RoleQueryUseCase
import com.vertyll.veds.iam.application.port.inbound.query.SecurityQueryUseCase
import com.vertyll.veds.iam.application.port.inbound.query.UserQueryUseCase
import com.vertyll.veds.iam.application.port.outbound.AuthEventPublisherPort
import com.vertyll.veds.iam.application.port.outbound.IdentityProviderPort
import com.vertyll.veds.iam.application.port.outbound.RolePermissionsEventPublisherPort
import com.vertyll.veds.iam.application.service.command.PermissionCatalogueService
import com.vertyll.veds.iam.application.service.command.RoleCommandService
import com.vertyll.veds.iam.application.service.command.SecurityCommandService
import com.vertyll.veds.iam.application.service.command.UserCommandService
import com.vertyll.veds.iam.application.service.command.UserProvisioningService
import com.vertyll.veds.iam.application.service.query.AuthQueryService
import com.vertyll.veds.iam.application.service.query.PermissionQueryService
import com.vertyll.veds.iam.application.service.query.RoleQueryService
import com.vertyll.veds.iam.application.service.query.SecurityQueryService
import com.vertyll.veds.iam.application.service.query.UserQueryService
import com.vertyll.veds.iam.domain.repository.PermissionRepository
import com.vertyll.veds.iam.domain.repository.RoleRepository
import com.vertyll.veds.iam.domain.repository.UserRepository
import com.vertyll.veds.iam.infrastructure.transaction.TransactionalUseCaseFactory
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
    fun roleCommandUseCase(
        transactions: TransactionalUseCaseFactory,
        roleRepository: RoleRepository,
        permissionRepository: PermissionRepository,
        userRepository: UserRepository,
        identityProvider: IdentityProviderPort,
        rolePermissionsEventPublisher: RolePermissionsEventPublisherPort,
    ): RoleCommandUseCase =
        transactions.wrap(
            RoleCommandUseCase::class.java,
            RoleCommandService(
                roleRepository,
                permissionRepository,
                userRepository,
                identityProvider,
                rolePermissionsEventPublisher,
            ),
            NO_METHODS,
        )

    @Bean
    fun permissionCatalogueUseCase(
        transactions: TransactionalUseCaseFactory,
        permissionRepository: PermissionRepository,
        roleRepository: RoleRepository,
        rolePermissionsEventPublisher: RolePermissionsEventPublisherPort,
    ): PermissionCatalogueUseCase =
        transactions.wrap(
            PermissionCatalogueUseCase::class.java,
            PermissionCatalogueService(
                permissionRepository,
                roleRepository,
                rolePermissionsEventPublisher,
            ),
            NO_METHODS,
        )

    @Bean
    fun securityCommandUseCase(
        transactions: TransactionalUseCaseFactory,
        identityProvider: IdentityProviderPort,
    ): SecurityCommandUseCase =
        transactions.wrap(
            SecurityCommandUseCase::class.java,
            SecurityCommandService(
                identityProvider,
            ),
            NO_METHODS,
        )

    @Bean
    fun userCommandUseCase(
        transactions: TransactionalUseCaseFactory,
        userRepository: UserRepository,
        authEventPublisher: AuthEventPublisherPort,
    ): UserCommandUseCase =
        transactions.wrap(
            UserCommandUseCase::class.java,
            UserCommandService(
                userRepository,
                authEventPublisher,
            ),
            NO_METHODS,
        )

    @Bean
    fun provisionCurrentUserUseCase(
        transactions: TransactionalUseCaseFactory,
        userRepository: UserRepository,
        roleRepository: RoleRepository,
        authEventPublisher: AuthEventPublisherPort,
    ): ProvisionCurrentUserUseCase =
        transactions.wrap(
            ProvisionCurrentUserUseCase::class.java,
            UserProvisioningService(
                userRepository,
                roleRepository,
                authEventPublisher,
            ),
            NO_METHODS,
        )

    @Bean
    fun authQueryUseCase(
        transactions: TransactionalUseCaseFactory,
        userRepository: UserRepository,
    ): AuthQueryUseCase =
        transactions.wrap(
            AuthQueryUseCase::class.java,
            AuthQueryService(
                userRepository,
            ),
            ALL_METHODS,
        )

    @Bean
    fun permissionQueryUseCase(
        transactions: TransactionalUseCaseFactory,
        permissionRepository: PermissionRepository,
        roleRepository: RoleRepository,
    ): PermissionQueryUseCase =
        transactions.wrap(
            PermissionQueryUseCase::class.java,
            PermissionQueryService(
                permissionRepository,
                roleRepository,
            ),
            ALL_METHODS,
        )

    @Bean
    fun roleQueryUseCase(
        transactions: TransactionalUseCaseFactory,
        roleRepository: RoleRepository,
        userRepository: UserRepository,
    ): RoleQueryUseCase =
        transactions.wrap(
            RoleQueryUseCase::class.java,
            RoleQueryService(
                roleRepository,
                userRepository,
            ),
            ALL_METHODS,
        )

    @Bean
    fun securityQueryUseCase(
        transactions: TransactionalUseCaseFactory,
        identityProvider: IdentityProviderPort,
    ): SecurityQueryUseCase =
        transactions.wrap(
            SecurityQueryUseCase::class.java,
            SecurityQueryService(
                identityProvider,
            ),
            ALL_METHODS,
        )

    @Bean
    fun userQueryUseCase(
        transactions: TransactionalUseCaseFactory,
        userRepository: UserRepository,
    ): UserQueryUseCase =
        transactions.wrap(
            UserQueryUseCase::class.java,
            UserQueryService(
                userRepository,
            ),
            ALL_METHODS,
        )
}
