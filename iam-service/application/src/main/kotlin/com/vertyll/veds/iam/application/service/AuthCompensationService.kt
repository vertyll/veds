package com.vertyll.veds.iam.application.service

import com.vertyll.veds.iam.application.port.inbound.AuthCompensationUseCase
import com.vertyll.veds.iam.application.port.outbound.IdentityProviderPort
import com.vertyll.veds.iam.application.saga.model.AuthCompensationCommand
import com.vertyll.veds.iam.domain.repository.UserRepository
import com.vertyll.veds.iam.domain.repository.VerificationTokenRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

/**
 * Dispatches typed [AuthCompensationCommand]s onto the appropriate domain
 * repositories / outbound ports. Exhaustive `when` over the sealed
 * hierarchy — adding a new compensation action becomes a compile-time
 * error rather than a runtime NPE.
 *
 * Intentionally does NOT swallow exceptions — propagating them lets the
 * inbound Kafka listener trigger broker-level retry / DLT, and the
 * `SagaWatchdog` still provides a slower cooldown-based safety net.
 */
@Service
internal class AuthCompensationService(
    private val userRepository: UserRepository,
    private val verificationTokenRepository: VerificationTokenRepository,
    private val identityProvider: IdentityProviderPort,
) : AuthCompensationUseCase {
    private val logger = LoggerFactory.getLogger(javaClass)

    override fun compensate(command: AuthCompensationCommand) {
        when (command) {
            is AuthCompensationCommand.DeleteUser -> deleteUser(command)
            is AuthCompensationCommand.DeleteVerificationToken -> deleteVerificationToken(command)
            is AuthCompensationCommand.RevertPasswordUpdate -> revertPasswordUpdate(command)
            is AuthCompensationCommand.RevertEmailUpdate -> revertEmailUpdate(command)
        }
    }

    private fun deleteUser(command: AuthCompensationCommand.DeleteUser) {
        userRepository.findById(command.userId)?.let {
            logger.info("Compensating CreateUser step: deleting user with ID {}", command.userId)
            userRepository.deleteById(command.userId)
        } ?: logger.info(
            "Compensation no-op: user {} already absent (already-compensated or never persisted)",
            command.userId,
        )
    }

    private fun deleteVerificationToken(command: AuthCompensationCommand.DeleteVerificationToken) {
        verificationTokenRepository.findById(command.tokenId)?.let {
            logger.info(
                "Compensating CreateVerificationToken step: deleting token with ID {}",
                command.tokenId,
            )
            verificationTokenRepository.deleteById(command.tokenId)
        } ?: logger.info(
            "Compensation no-op: verification token {} already absent",
            command.tokenId,
        )
    }

    private fun revertPasswordUpdate(command: AuthCompensationCommand.RevertPasswordUpdate) {
        // Passwords are owned by Keycloak — a true rollback would require
        // the previous hash, which we intentionally do NOT carry on the
        // wire. We surface the situation in logs for manual remediation.
        logger.warn(
            "Cannot revert password change for user {} — passwords are managed by Keycloak and the " +
                "previous credential is not retained. Manual intervention may be required.",
            command.userId,
        )
    }

    private fun revertEmailUpdate(command: AuthCompensationCommand.RevertEmailUpdate) {
        userRepository.findById(command.userId)?.let { user ->
            logger.info(
                "Compensating UpdateEmail step: reverting email for user ID {} to {}",
                command.userId,
                command.originalEmail,
            )
            user.keycloakId?.let { identityProvider.updateEmail(it, command.originalEmail) }
            userRepository.save(user.withEmail(command.originalEmail))
        } ?: logger.warn(
            "Cannot revert email for user {} — user no longer exists locally",
            command.userId,
        )
    }
}
