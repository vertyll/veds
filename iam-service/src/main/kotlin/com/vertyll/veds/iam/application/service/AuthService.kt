package com.vertyll.veds.iam.application.service

import com.vertyll.veds.iam.application.port.out.AuthEventPublisherPort
import com.vertyll.veds.iam.application.port.out.IdentityProviderPort
import com.vertyll.veds.iam.application.saga.model.SagaStepNames
import com.vertyll.veds.iam.application.saga.model.SagaTypes
import com.vertyll.veds.iam.application.saga.port.SagaProcessPort
import com.vertyll.veds.iam.domain.model.EmailTemplate
import com.vertyll.veds.iam.domain.model.TokenTypes
import com.vertyll.veds.iam.domain.model.VerificationToken
import com.vertyll.veds.iam.domain.repository.RoleRepository
import com.vertyll.veds.iam.domain.repository.UserRepository
import com.vertyll.veds.iam.domain.repository.VerificationTokenRepository
import com.vertyll.veds.iam.infrastructure.exception.ApiException
import com.vertyll.veds.iam.infrastructure.web.dto.ChangeEmailRequestDto
import com.vertyll.veds.iam.infrastructure.web.dto.ChangePasswordRequestDto
import com.vertyll.veds.iam.infrastructure.web.dto.RegisterRequestDto
import com.vertyll.veds.iam.infrastructure.web.dto.ResetPasswordRequestDto
import com.vertyll.veds.sharedinfrastructure.event.mail.MailRequestedEvent
import com.vertyll.veds.sharedinfrastructure.role.RoleType
import com.vertyll.veds.sharedinfrastructure.saga.enums.SagaStepStatus
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.util.UUID
import com.vertyll.veds.iam.domain.model.User as DomainUser

@Service
class AuthService(
    private val verificationTokenRepository: VerificationTokenRepository,
    private val userRepository: UserRepository,
    private val roleRepository: RoleRepository,
    private val identityProvider: IdentityProviderPort,
    private val authEventPublisher: AuthEventPublisherPort,
    private val sagaProcessPort: SagaProcessPort,
) {
    private val logger: Logger = LoggerFactory.getLogger(AuthService::class.java)

    private companion object {
        private const val USER_NOT_FOUND = "User not found"
        private const val INVALID_TOKEN = "Invalid token"
        private const val TOKEN_EXPIRED_OR_USED = "Token expired or already used"
        private const val INVALID_PASSWORD = "Invalid password"
        private const val MISSING_NEW_EMAIL_DATA = "Missing new email data"
        private const val INVALID_CONFIRMATION_CODE = "Invalid confirmation code"
        private const val INVALID_TOKEN_ID = "Invalid token ID"
        private const val INVALID_CURRENT_PASSWORD = "Invalid current password"
        private const val REGISTRATION_FAILED = "Registration failed. Please check your information and try again."
        private const val CANNOT_CHANGE_EMAIL = "Cannot change to this email address"
        private const val DEFAULT_ROLE_NOT_FOUND = "Default USER role not found in system"
        private const val DEFAULT_VERIFICATION_TOKEN_EXPIRY_HOURS = 24L
    }

    @Transactional
    fun register(request: RegisterRequestDto) {
        if (userRepository.existsByEmail(request.email)) {
            logger.warn("Registration attempted with existing email: {}", request.email)
            throw ApiException(REGISTRATION_FAILED, HttpStatus.BAD_REQUEST)
        }

        val saga =
            sagaProcessPort.beginSaga(
                sagaType = SagaTypes.USER_REGISTRATION,
                payload =
                    mapOf(
                        "email" to request.email,
                        "firstName" to request.firstName,
                        "lastName" to request.lastName,
                    ),
            )

        try {
            logger.info("Creating new user with email: {}", request.email)

            val keycloakId =
                identityProvider.createUser(
                    email = request.email,
                    password = request.password,
                    firstName = request.firstName,
                    lastName = request.lastName,
                    roleName = RoleType.USER.value,
                )

            val userRole =
                roleRepository.findByName(RoleType.USER.value)
                    ?: throw ApiException(DEFAULT_ROLE_NOT_FOUND, HttpStatus.INTERNAL_SERVER_ERROR)

            val newUser =
                DomainUser
                    .create(
                        keycloakId = keycloakId,
                        email = request.email,
                        firstName = request.firstName,
                        lastName = request.lastName,
                    ).withRole(userRole)

            val savedUser = userRepository.save(newUser)

            sagaProcessPort.appendSagaStep(
                sagaId = saga.id,
                stepName = SagaStepNames.CREATE_USER,
                status = SagaStepStatus.COMPLETED,
                payload =
                    mapOf(
                        "userId" to savedUser.id!!,
                        "email" to savedUser.email,
                        "keycloakId" to keycloakId.toString(),
                    ),
            )

            sagaProcessPort.appendSagaStep(
                sagaId = saga.id,
                stepName = SagaStepNames.CREATE_USER_EVENT,
                status = SagaStepStatus.COMPLETED,
            )

            val token = generateRandomToken()
            val verificationToken = saveVerificationToken(savedUser.email, token, TokenTypes.ACCOUNT_ACTIVATION.value, sagaId = saga.id)

            sagaProcessPort.appendSagaStep(
                sagaId = saga.id,
                stepName = SagaStepNames.CREATE_VERIFICATION_TOKEN,
                status = SagaStepStatus.COMPLETED,
                payload =
                    mapOf(
                        "tokenId" to verificationToken.id!!,
                        "token" to token,
                    ),
            )

            authEventPublisher.sendMailRequestedEvent(
                MailRequestedEvent(
                    to = savedUser.email,
                    subject = "Activate your account",
                    templateName = EmailTemplate.ACTIVATE_ACCOUNT.name,
                    variables =
                        mapOf(
                            "firstName" to savedUser.firstName,
                            "activationToken" to token,
                        ),
                    sagaId = saga.id,
                ),
            )

            sagaProcessPort.appendSagaStep(
                sagaId = saga.id,
                stepName = SagaStepNames.CREATE_MAIL_EVENT,
                status = SagaStepStatus.COMPLETED,
            )

            sagaProcessPort.markAwaitingResponse(saga.id)
        } catch (e: Exception) {
            logger.error("User registration failed for {}: {}", request.email, e.message, e)
            throw e
        }
    }

    @Transactional
    fun activateAccount(token: String) {
        val verificationToken =
            verificationTokenRepository.findByToken(token)
                ?: throw ApiException(INVALID_TOKEN, HttpStatus.BAD_REQUEST)

        validateVerificationToken(verificationToken)

        val user =
            userRepository.findByEmail(verificationToken.username)
                ?: throw ApiException(USER_NOT_FOUND, HttpStatus.NOT_FOUND)

        user.keycloakId?.let { identityProvider.enableUser(it) }

        verificationTokenRepository.save(verificationToken.markUsed())
    }

    @Transactional
    fun resendActivationEmail(email: String) {
        val user = userRepository.findByEmail(email)
        if (user == null) {
            logger.info("Activation email resend requested for non-existent email: {}", email)
            return
        }

        val saga =
            sagaProcessPort.beginSaga(
                sagaType = SagaTypes.EMAIL_VERIFICATION,
                payload =
                    mapOf(
                        "email" to email,
                        "userId" to user.id!!,
                    ),
            )

        try {
            val token = generateRandomToken()
            val verificationToken = saveVerificationToken(user.email, token, TokenTypes.ACCOUNT_ACTIVATION.value, sagaId = saga.id)

            sagaProcessPort.appendSagaStep(
                sagaId = saga.id,
                stepName = SagaStepNames.CREATE_VERIFICATION_TOKEN,
                status = SagaStepStatus.COMPLETED,
                payload =
                    mapOf(
                        "tokenId" to verificationToken.id!!,
                        "token" to token,
                    ),
            )

            authEventPublisher.sendMailRequestedEvent(
                MailRequestedEvent(
                    to = user.email,
                    subject = "Activate your account",
                    templateName = EmailTemplate.ACTIVATE_ACCOUNT.name,
                    variables =
                        mapOf(
                            "firstName" to user.firstName,
                            "activationToken" to token,
                        ),
                    sagaId = saga.id,
                ),
            )

            sagaProcessPort.appendSagaStep(
                sagaId = saga.id,
                stepName = SagaStepNames.CREATE_MAIL_EVENT,
                status = SagaStepStatus.COMPLETED,
            )

            sagaProcessPort.markAwaitingResponse(saga.id)
        } catch (e: Exception) {
            logger.error("Resend activation email failed for {}: {}", email, e.message, e)
            throw e
        }
    }

    @Transactional
    fun sendPasswordResetRequest(email: String) {
        val user = userRepository.findByEmail(email)
        if (user == null) {
            logger.info("Password reset requested for non-existent email: {}", email)
            return
        }

        val saga =
            sagaProcessPort.beginSaga(
                sagaType = SagaTypes.PASSWORD_RESET,
                payload =
                    mapOf(
                        "email" to email,
                        "userId" to user.id!!,
                    ),
            )

        try {
            val token = generateRandomToken()
            val verificationToken = saveVerificationToken(user.email, token, TokenTypes.PASSWORD_RESET.value, sagaId = saga.id)

            sagaProcessPort.appendSagaStep(
                sagaId = saga.id,
                stepName = SagaStepNames.CREATE_RESET_TOKEN,
                status = SagaStepStatus.COMPLETED,
                payload =
                    mapOf(
                        "tokenId" to verificationToken.id!!,
                        "token" to token,
                    ),
            )

            authEventPublisher.sendMailRequestedEvent(
                MailRequestedEvent(
                    to = user.email,
                    subject = "Password Reset Request",
                    templateName = EmailTemplate.RESET_PASSWORD.name,
                    variables =
                        mapOf(
                            "firstName" to user.firstName,
                            "resetToken" to token,
                        ),
                    sagaId = saga.id,
                ),
            )

            sagaProcessPort.appendSagaStep(
                sagaId = saga.id,
                stepName = SagaStepNames.CREATE_MAIL_EVENT,
                status = SagaStepStatus.COMPLETED,
            )

            sagaProcessPort.markAwaitingResponse(saga.id)
        } catch (e: Exception) {
            logger.error("Password reset request failed for {}: {}", email, e.message, e)
            throw e
        }
    }

    @Transactional
    fun resetPassword(
        token: String,
        request: ResetPasswordRequestDto,
    ) {
        val verificationToken =
            verificationTokenRepository.findByToken(token)
                ?: throw ApiException(INVALID_TOKEN, HttpStatus.BAD_REQUEST)
        if (verificationToken.tokenType != TokenTypes.PASSWORD_RESET.value) {
            throw ApiException(INVALID_TOKEN, HttpStatus.BAD_REQUEST)
        }

        validateVerificationToken(verificationToken)

        val user =
            userRepository.findByEmail(verificationToken.username)
                ?: throw ApiException(USER_NOT_FOUND, HttpStatus.NOT_FOUND)

        user.keycloakId?.let { identityProvider.resetPassword(it, request.newPassword) }

        verificationTokenRepository.save(verificationToken.markUsed())
    }

    @Transactional
    fun requestEmailChange(
        email: String,
        request: ChangeEmailRequestDto,
    ) {
        val user =
            userRepository.findByEmail(email)
                ?: throw ApiException(USER_NOT_FOUND, HttpStatus.NOT_FOUND)

        if (!identityProvider.validatePassword(email, request.password)) {
            throw ApiException(INVALID_PASSWORD, HttpStatus.UNAUTHORIZED)
        }

        if (userRepository.existsByEmail(request.newEmail)) {
            logger.warn("Email change requested to already existing email: {}", request.newEmail)
            throw ApiException(CANNOT_CHANGE_EMAIL, HttpStatus.BAD_REQUEST)
        }

        val saga =
            sagaProcessPort.beginSaga(
                sagaType = SagaTypes.EMAIL_CHANGE,
                payload =
                    mapOf(
                        "email" to email,
                        "newEmail" to request.newEmail,
                        "userId" to user.id!!,
                    ),
            )

        try {
            val token = generateRandomToken()
            val verificationToken = saveVerificationToken(user.email, token, TokenTypes.EMAIL_CHANGE.value, request.newEmail, saga.id)

            sagaProcessPort.appendSagaStep(
                sagaId = saga.id,
                stepName = SagaStepNames.CREATE_VERIFICATION_TOKEN,
                status = SagaStepStatus.COMPLETED,
                payload =
                    mapOf(
                        "tokenId" to verificationToken.id!!,
                        "token" to token,
                        "sagaId" to saga.id,
                    ),
            )

            authEventPublisher.sendMailRequestedEvent(
                MailRequestedEvent(
                    to = request.newEmail,
                    subject = "Confirm Email Change",
                    templateName = EmailTemplate.CHANGE_EMAIL.name,
                    variables =
                        mapOf(
                            "firstName" to user.firstName,
                            "confirmationToken" to token,
                        ),
                    sagaId = saga.id,
                ),
            )

            sagaProcessPort.appendSagaStep(
                sagaId = saga.id,
                stepName = SagaStepNames.CREATE_MAIL_EVENT,
                status = SagaStepStatus.COMPLETED,
            )

            sagaProcessPort.markAwaitingResponse(saga.id)
        } catch (e: Exception) {
            logger.error("Email change request failed for {}: {}", email, e.message, e)
            throw e
        }
    }

    @Transactional
    fun confirmEmailChange(token: String) {
        val verificationToken =
            verificationTokenRepository.findByToken(token)
                ?: throw ApiException(INVALID_TOKEN, HttpStatus.BAD_REQUEST)
        if (verificationToken.tokenType != TokenTypes.EMAIL_CHANGE.value) {
            throw ApiException(INVALID_TOKEN, HttpStatus.BAD_REQUEST)
        }

        validateVerificationToken(verificationToken)

        val user =
            userRepository.findByEmail(verificationToken.username)
                ?: throw ApiException(USER_NOT_FOUND, HttpStatus.NOT_FOUND)

        val newEmail =
            verificationToken.additionalData
                ?: throw ApiException(MISSING_NEW_EMAIL_DATA, HttpStatus.INTERNAL_SERVER_ERROR)

        val originalEmail = user.email

        user.keycloakId?.let { identityProvider.updateEmail(it, newEmail) }
        userRepository.save(user.withEmail(newEmail))

        verificationTokenRepository.save(verificationToken.markUsed())

        verificationToken.sagaId?.let { sagaId ->
            sagaProcessPort.appendSagaStep(
                sagaId = sagaId,
                stepName = SagaStepNames.UPDATE_EMAIL,
                status = SagaStepStatus.COMPLETED,
                payload =
                    mapOf(
                        "userId" to user.id!!,
                        "originalEmail" to originalEmail,
                        "newEmail" to newEmail,
                    ),
            )
            sagaProcessPort.markSagaCompleted(sagaId)
        }
    }

    @Transactional
    fun changePassword(
        email: String,
        request: ChangePasswordRequestDto,
    ) {
        val user =
            userRepository.findByEmail(email)
                ?: throw ApiException(USER_NOT_FOUND, HttpStatus.NOT_FOUND)

        val saga =
            sagaProcessPort.beginSaga(
                sagaType = SagaTypes.PASSWORD_CHANGE,
                payload =
                    mapOf(
                        "email" to email,
                        "userId" to user.id!!,
                    ),
            )

        try {
            if (!identityProvider.validatePassword(email, request.currentPassword)) {
                throw ApiException(INVALID_CURRENT_PASSWORD, HttpStatus.UNAUTHORIZED)
            }

            sagaProcessPort.appendSagaStep(
                sagaId = saga.id,
                stepName = SagaStepNames.VERIFY_CURRENT_PASSWORD,
                status = SagaStepStatus.COMPLETED,
            )

            val token = generateRandomToken()
            val verificationToken = saveVerificationToken(user.email, token, TokenTypes.PASSWORD_CHANGE_REQUEST.value, null, saga.id)

            sagaProcessPort.appendSagaStep(
                sagaId = saga.id,
                stepName = SagaStepNames.CREATE_VERIFICATION_TOKEN,
                status = SagaStepStatus.COMPLETED,
                payload =
                    mapOf(
                        "tokenId" to verificationToken.id!!,
                        "token" to token,
                        "sagaId" to saga.id,
                    ),
            )

            authEventPublisher.sendMailRequestedEvent(
                MailRequestedEvent(
                    to = user.email,
                    subject = "Confirm Password Change",
                    templateName = EmailTemplate.SET_NEW_PASSWORD.name,
                    variables =
                        mapOf(
                            "firstName" to user.firstName,
                            "confirmationToken" to token,
                        ),
                    sagaId = saga.id,
                ),
            )

            sagaProcessPort.appendSagaStep(
                sagaId = saga.id,
                stepName = SagaStepNames.CREATE_MAIL_EVENT,
                status = SagaStepStatus.COMPLETED,
            )

            sagaProcessPort.markAwaitingResponse(saga.id)
        } catch (e: Exception) {
            logger.error("Password change request failed for {}: {}", email, e.message, e)
            throw e
        }
    }

    @Transactional
    fun confirmPasswordChange(
        token: String,
        newPassword: String,
    ) {
        val verificationToken =
            verificationTokenRepository.findByToken(token)
                ?: throw ApiException(INVALID_TOKEN, HttpStatus.BAD_REQUEST)
        if (verificationToken.tokenType != TokenTypes.PASSWORD_CHANGE_REQUEST.value) {
            throw ApiException(INVALID_TOKEN, HttpStatus.BAD_REQUEST)
        }

        validateVerificationToken(verificationToken)

        val user =
            userRepository.findByEmail(verificationToken.username)
                ?: throw ApiException(USER_NOT_FOUND, HttpStatus.NOT_FOUND)

        user.keycloakId?.let { identityProvider.resetPassword(it, newPassword) }

        verificationTokenRepository.save(verificationToken.markUsed())

        verificationToken.sagaId?.let { sagaId ->
            sagaProcessPort.appendSagaStep(
                sagaId = sagaId,
                stepName = SagaStepNames.UPDATE_PASSWORD,
                status = SagaStepStatus.COMPLETED,
                payload =
                    mapOf(
                        "userId" to user.id!!,
                    ),
            )
            sagaProcessPort.markSagaCompleted(sagaId)
        }
    }

    @Transactional
    fun setNewPassword(
        tokenId: Long,
        request: ResetPasswordRequestDto,
    ) {
        val verificationToken =
            verificationTokenRepository.findById(tokenId)
                ?: throw ApiException(INVALID_TOKEN_ID, HttpStatus.BAD_REQUEST)

        if (verificationToken.token != request.confirmationCode) {
            throw ApiException(INVALID_CONFIRMATION_CODE, HttpStatus.BAD_REQUEST)
        }

        validateVerificationToken(verificationToken)

        val user =
            userRepository.findByEmail(verificationToken.username)
                ?: throw ApiException(USER_NOT_FOUND, HttpStatus.NOT_FOUND)

        user.keycloakId?.let { identityProvider.resetPassword(it, request.newPassword) }

        verificationTokenRepository.save(verificationToken.markUsed())
    }

    @Transactional(readOnly = true)
    fun getUserPermissions(keycloakId: UUID): List<String> {
        val user =
            userRepository.findByKeycloakId(keycloakId)
                ?: throw ApiException(USER_NOT_FOUND, HttpStatus.NOT_FOUND)
        return user.permissions.map { it.name }
    }

    private fun saveVerificationToken(
        email: String,
        token: String,
        tokenType: String,
        additionalData: String? = null,
        sagaId: String? = null,
    ): VerificationToken {
        val expiryDate = LocalDateTime.now().plusHours(DEFAULT_VERIFICATION_TOKEN_EXPIRY_HOURS)
        val verificationToken =
            VerificationToken(
                token = token,
                username = email,
                expiryDate = expiryDate,
                tokenType = tokenType,
                additionalData = additionalData,
                sagaId = sagaId,
            )
        return verificationTokenRepository.save(verificationToken)
    }

    private fun validateVerificationToken(verificationToken: VerificationToken) {
        if (verificationToken.used || verificationToken.expiryDate.isBefore(LocalDateTime.now())) {
            throw ApiException(TOKEN_EXPIRED_OR_USED, HttpStatus.BAD_REQUEST)
        }
    }

    private fun generateRandomToken(): String = UUID.randomUUID().toString()
}
