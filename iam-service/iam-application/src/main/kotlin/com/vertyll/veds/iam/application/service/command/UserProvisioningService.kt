package com.vertyll.veds.iam.application.service.command

import com.vertyll.veds.iam.application.dto.AuthenticatedIdentity
import com.vertyll.veds.iam.application.port.inbound.command.ProvisionCurrentUserUseCase
import com.vertyll.veds.iam.application.port.outbound.AuthEventPublisherPort
import com.vertyll.veds.iam.domain.error.IamError
import com.vertyll.veds.iam.domain.model.RoleType
import com.vertyll.veds.iam.domain.model.User
import com.vertyll.veds.iam.domain.repository.RoleRepository
import com.vertyll.veds.iam.domain.repository.UserRepository
import com.vertyll.veds.sharederror.ApiException

class UserProvisioningService(
    private val userRepository: UserRepository,
    private val roleRepository: RoleRepository,
    private val authEventPublisher: AuthEventPublisherPort,
) : ProvisionCurrentUserUseCase {
    override fun provision(identity: AuthenticatedIdentity) {
        if (userRepository.findByKeycloakId(identity.keycloakId) != null) return

        val defaultRole =
            roleRepository.findByName(RoleType.USER.value)
                ?: throw ApiException(IamError.DEFAULT_ROLE_NOT_CONFIGURED)

        val user =
            userRepository.save(
                User
                    .create(
                        keycloakId = identity.keycloakId,
                        email = identity.email,
                        firstName = identity.firstName,
                        lastName = identity.lastName,
                    ).withRole(defaultRole),
            )

        authEventPublisher.publishUserRegistered(
            userId = identity.keycloakId,
            email = user.email,
            firstName = user.firstName,
            lastName = user.lastName,
        )
    }
}
