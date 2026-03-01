package com.vertyll.veds.iam.domain.service

import com.vertyll.veds.iam.domain.dto.AuthRequestDto
import com.vertyll.veds.iam.domain.dto.AuthResponseDto
import com.vertyll.veds.iam.domain.dto.ChangeEmailRequestDto
import com.vertyll.veds.iam.domain.dto.ChangePasswordRequestDto
import com.vertyll.veds.iam.domain.dto.RegisterRequestDto
import com.vertyll.veds.iam.domain.dto.ResetPasswordRequestDto
import com.vertyll.veds.iam.domain.dto.SessionDto
import com.vertyll.veds.iam.domain.model.entity.RefreshToken
import com.vertyll.veds.iam.domain.model.entity.User
import com.vertyll.veds.iam.domain.model.entity.VerificationToken
import com.vertyll.veds.iam.domain.model.enums.EmailTemplate
import com.vertyll.veds.iam.domain.model.enums.RoleType
import com.vertyll.veds.iam.domain.model.enums.SagaStepNames
import com.vertyll.veds.iam.domain.model.enums.SagaTypes
import com.vertyll.veds.iam.domain.model.enums.TokenTypes
import com.vertyll.veds.iam.domain.repository.RefreshTokenRepository
import com.vertyll.veds.iam.domain.repository.RoleRepository
import com.vertyll.veds.iam.domain.repository.UserRepository
import com.vertyll.veds.iam.domain.repository.VerificationTokenRepository
import com.vertyll.veds.iam.infrastructure.exception.ApiException
import com.vertyll.veds.iam.infrastructure.kafka.AuthEventProducer
import com.vertyll.veds.sharedinfrastructure.config.JwtConstants
import com.vertyll.veds.sharedinfrastructure.event.mail.MailRequestedEvent
import com.vertyll.veds.sharedinfrastructure.saga.enums.SagaStepStatus
import jakarta.servlet.http.Cookie
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.time.LocalDateTime
import java.util.UUID

@Service
class AuthService(
    private val refreshTokenRepository: RefreshTokenRepository,
    private val verificationTokenRepository: VerificationTokenRepository,
    private val userRepository: UserRepository,
    private val roleRepository: RoleRepository,
    private val jwtService: JwtService,
    private val authEventProducer: AuthEventProducer,
    private val passwordEncoder: PasswordEncoder,
    private val sagaManager: SagaManager,
) {
    private val logger: Logger = LoggerFactory.getLogger(AuthService::class.java)

    private companion object {
        private const val PASSWORD_ENCODING_FAILED = "Password encoding failed"
        private const val USER_NOT_FOUND = "User not found"
        private const val INVALID_TOKEN = "Invalid token"
        private const val TOKEN_EXPIRED_OR_USED = "Token expired or already used"
        private const val INVALID_CREDENTIALS = "Invalid credentials"
        private const val ACCOUNT_NOT_ACTIVATED = "Account not activated"
        private const val REFRESH_TOKEN_MISSING = "Refresh token missing"
        private const val INVALID_OR_EXPIRED_REFRESH_TOKEN = "Invalid or expired refresh token"
        private const val INVALID_PASSWORD = "Invalid password"
        private const val MISSING_NEW_EMAIL_DATA = "Missing new email data"
        private const val INVALID_CONFIRMATION_CODE = "Invalid confirmation code"
        private const val INVALID_TOKEN_ID = "Invalid token ID"
        private const val INVALID_CURRENT_PASSWORD = "Invalid current password"
        private const val REGISTRATION_FAILED = "Registration failed. Please check your information and try again."
        private const val CANNOT_CHANGE_EMAIL = "Cannot change to this email address"
        private const val DEFAULT_ROLE_NOT_FOUND = "Default USER role not found in system"
        private const val DEFAULT_VERIFICATION_TOKEN_EXPIRY_HOURS = 24L
        private const val MILLISECONDS_IN_SECOND = 1000
    }

    @Transactional
    fun register(request: RegisterRequestDto) {
        if (userRepository.existsByEmail(request.email)) {
            logger.warn("Registration attempted with existing email: {}", request.email)
            throw ApiException(
                message = REGISTRATION_FAILED,
                status = HttpStatus.BAD_REQUEST,
            )
        }

        val saga =
            sagaManager.startSaga(
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
            val passwordHash =
                passwordEncoder.encode(request.password)
                    ?: throw ApiException(PASSWORD_ENCODING_FAILED, HttpStatus.INTERNAL_SERVER_ERROR)

            val user =
                User.create(
                    email = request.email,
                    password = passwordHash,
                    firstName = request.firstName,
                    lastName = request.lastName,
                    enabled = false,
                )

            val userRole =
                roleRepository
                    .findByName(RoleType.USER.value)
                    .orElseThrow {
                        ApiException(
                            DEFAULT_ROLE_NOT_FOUND,
                            HttpStatus.INTERNAL_SERVER_ERROR,
                        )
                    }
            user.addRole(userRole)

            userRepository.save(user)

            sagaManager.recordSagaStep(
                sagaId = saga.id,
                stepName = SagaStepNames.CREATE_USER,
                status = SagaStepStatus.COMPLETED,
                payload =
                    mapOf(
                        "userId" to user.id!!,
                        "email" to user.getEmail(),
                    ),
            )

            sagaManager.recordSagaStep(
                sagaId = saga.id,
                stepName = SagaStepNames.CREATE_USER_EVENT,
                status = SagaStepStatus.COMPLETED,
            )

            val token = UUID.randomUUID().toString()
            val verificationToken = saveVerificationToken(user.getEmail(), token, TokenTypes.ACCOUNT_ACTIVATION.value, sagaId = saga.id)

            sagaManager.recordSagaStep(
                sagaId = saga.id,
                stepName = SagaStepNames.CREATE_VERIFICATION_TOKEN,
                status = SagaStepStatus.COMPLETED,
                payload =
                    mapOf(
                        "tokenId" to verificationToken.id!!,
                        "token" to token,
                    ),
            )

            authEventProducer.sendMailRequestedEvent(
                MailRequestedEvent(
                    to = user.getEmail(),
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

            sagaManager.recordSagaStep(
                sagaId = saga.id,
                stepName = SagaStepNames.CREATE_MAIL_EVENT,
                status = SagaStepStatus.COMPLETED,
            )

            sagaManager.awaitResponse(saga.id)
        } catch (e: Exception) {
            logger.error("User registration failed for {}: {}", request.email, e.message, e)
            throw e
        }
    }

    @Transactional
    fun activateAccount(token: String) {
        val verificationToken =
            verificationTokenRepository
                .findByToken(token)
                .orElseThrow { ApiException(INVALID_TOKEN, HttpStatus.BAD_REQUEST) }

        if (verificationToken.used || verificationToken.expiryDate.isBefore(LocalDateTime.now())) {
            throw ApiException(TOKEN_EXPIRED_OR_USED, HttpStatus.BAD_REQUEST)
        }

        val user =
            userRepository
                .findByEmail(verificationToken.username)
                .orElseThrow { ApiException(USER_NOT_FOUND, HttpStatus.NOT_FOUND) }

        user.enabled = true
        userRepository.save(user)

        verificationToken.used = true
        verificationTokenRepository.save(verificationToken)
    }

    @Transactional
    fun resendActivationEmail(email: String) {
        val userOptional = userRepository.findByEmail(email)

        if (userOptional.isEmpty) {
            logger.info("Activation email resend requested for non-existent email: {}", email)
            return
        }

        val user = userOptional.get()

        if (user.isEnabled) {
            logger.info("Activation email resend requested for already activated account: {}", email)
            return
        }

        val saga =
            sagaManager.startSaga(
                sagaType = SagaTypes.EMAIL_VERIFICATION,
                payload =
                    mapOf(
                        "email" to email,
                        "userId" to user.id!!,
                    ),
            )

        try {
            val token = UUID.randomUUID().toString()
            val verificationToken = saveVerificationToken(user.getEmail(), token, TokenTypes.ACCOUNT_ACTIVATION.value, sagaId = saga.id)

            sagaManager.recordSagaStep(
                sagaId = saga.id,
                stepName = SagaStepNames.CREATE_VERIFICATION_TOKEN,
                status = SagaStepStatus.COMPLETED,
                payload =
                    mapOf(
                        "tokenId" to verificationToken.id!!,
                        "token" to token,
                    ),
            )

            authEventProducer.sendMailRequestedEvent(
                MailRequestedEvent(
                    to = user.getEmail(),
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

            sagaManager.recordSagaStep(
                sagaId = saga.id,
                stepName = SagaStepNames.CREATE_MAIL_EVENT,
                status = SagaStepStatus.COMPLETED,
            )

            sagaManager.awaitResponse(saga.id)
        } catch (e: Exception) {
            logger.error("Resend activation email failed for {}: {}", email, e.message, e)
            throw e
        }
    }

    fun authenticate(
        request: AuthRequestDto,
        response: HttpServletResponse,
    ): AuthResponseDto {
        val user =
            userRepository
                .findByEmail(request.email)
                .orElseThrow { ApiException(USER_NOT_FOUND, HttpStatus.UNAUTHORIZED) }

        if (!passwordEncoder.matches(request.password, user.password)) {
            throw ApiException(INVALID_CREDENTIALS, HttpStatus.UNAUTHORIZED)
        }

        if (!user.isEnabled) {
            throw ApiException(ACCOUNT_NOT_ACTIVATED, HttpStatus.FORBIDDEN)
        }

        val accessToken = jwtService.generateToken(user)
        val refreshToken = jwtService.generateRefreshToken(user)

        val refreshTokenEntity =
            RefreshToken(
                token = refreshToken,
                username = user.getEmail(),
                expiryDate = Instant.now().plusMillis(jwtService.getRefreshTokenExpirationTime()),
                revoked = false,
                deviceInfo = request.deviceInfo ?: request.userAgent,
            )
        refreshTokenRepository.save(refreshTokenEntity)

        addRefreshTokenCookie(response, refreshToken)

        return AuthResponseDto(
            token = accessToken,
            type = JwtConstants.BEARER_PREFIX,
            expiresIn = jwtService.getAccessTokenExpirationTime() / MILLISECONDS_IN_SECOND,
        )
    }

    fun refreshToken(request: HttpServletRequest): AuthResponseDto {
        val refreshToken =
            extractRefreshTokenFromCookies(request)
                ?: throw ApiException(REFRESH_TOKEN_MISSING, HttpStatus.UNAUTHORIZED)

        val refreshTokenEntity =
            refreshTokenRepository
                .findByToken(refreshToken)
                .filter { !it.revoked && it.expiryDate.isAfter(Instant.now()) }
                .orElseThrow { ApiException(INVALID_OR_EXPIRED_REFRESH_TOKEN, HttpStatus.UNAUTHORIZED) }

        val user =
            userRepository
                .findByEmail(refreshTokenEntity.username)
                .orElseThrow { ApiException(USER_NOT_FOUND, HttpStatus.UNAUTHORIZED) }

        val newAccessToken = jwtService.generateToken(user)
        return AuthResponseDto(
            token = newAccessToken,
            type = JwtConstants.BEARER_PREFIX,
            expiresIn = jwtService.getAccessTokenExpirationTime() / MILLISECONDS_IN_SECOND,
        )
    }

    @Transactional
    fun logout(
        request: HttpServletRequest,
        response: HttpServletResponse,
    ) {
        val refreshToken = extractRefreshTokenFromCookies(request)
        if (refreshToken != null) {
            refreshTokenRepository.findByToken(refreshToken).ifPresent {
                it.revoked = true
                refreshTokenRepository.save(it)
            }
        }
        deleteRefreshTokenCookie(response)
        SecurityContextHolder.clearContext()
    }

    @Transactional
    fun sendPasswordResetRequest(email: String) {
        val userOptional = userRepository.findByEmail(email)

        if (userOptional.isEmpty) {
            logger.info("Password reset requested for non-existent email: {}", email)
            return
        }

        val user = userOptional.get()

        val saga =
            sagaManager.startSaga(
                sagaType = SagaTypes.PASSWORD_RESET,
                payload =
                    mapOf(
                        "email" to email,
                        "userId" to user.id!!,
                    ),
            )

        try {
            val token = UUID.randomUUID().toString()
            val verificationToken = saveVerificationToken(user.getEmail(), token, TokenTypes.PASSWORD_RESET.value, sagaId = saga.id)

            sagaManager.recordSagaStep(
                sagaId = saga.id,
                stepName = SagaStepNames.CREATE_RESET_TOKEN,
                status = SagaStepStatus.COMPLETED,
                payload =
                    mapOf(
                        "tokenId" to verificationToken.id!!,
                        "token" to token,
                    ),
            )

            authEventProducer.sendMailRequestedEvent(
                MailRequestedEvent(
                    to = user.getEmail(),
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

            sagaManager.recordSagaStep(
                sagaId = saga.id,
                stepName = SagaStepNames.CREATE_MAIL_EVENT,
                status = SagaStepStatus.COMPLETED,
            )

            sagaManager.awaitResponse(saga.id)
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
            verificationTokenRepository
                .findByToken(token)
                .filter { it.tokenType == TokenTypes.PASSWORD_RESET.value }
                .orElseThrow { ApiException(INVALID_TOKEN, HttpStatus.BAD_REQUEST) }

        if (verificationToken.used || verificationToken.expiryDate.isBefore(LocalDateTime.now())) {
            throw ApiException(TOKEN_EXPIRED_OR_USED, HttpStatus.BAD_REQUEST)
        }

        val user =
            userRepository
                .findByEmail(verificationToken.username)
                .orElseThrow { ApiException(USER_NOT_FOUND, HttpStatus.NOT_FOUND) }

        val encodedPassword =
            passwordEncoder.encode(request.newPassword)
                ?: throw ApiException(PASSWORD_ENCODING_FAILED, HttpStatus.INTERNAL_SERVER_ERROR)
        user.setPassword(encodedPassword)
        userRepository.save(user)

        verificationToken.used = true
        verificationTokenRepository.save(verificationToken)
    }

    @Transactional
    fun requestEmailChange(
        email: String,
        request: ChangeEmailRequestDto,
    ) {
        val user =
            userRepository
                .findByEmail(email)
                .orElseThrow { ApiException(USER_NOT_FOUND, HttpStatus.NOT_FOUND) }

        if (!passwordEncoder.matches(request.password, user.password)) {
            throw ApiException(INVALID_PASSWORD, HttpStatus.UNAUTHORIZED)
        }

        if (userRepository.existsByEmail(request.newEmail)) {
            logger.warn("Email change requested to already existing email: {}", request.newEmail)
            throw ApiException(CANNOT_CHANGE_EMAIL, HttpStatus.BAD_REQUEST)
        }

        val saga =
            sagaManager.startSaga(
                sagaType = SagaTypes.EMAIL_CHANGE,
                payload =
                    mapOf(
                        "email" to email,
                        "newEmail" to request.newEmail,
                        "userId" to user.id!!,
                    ),
            )

        try {
            val token = UUID.randomUUID().toString()
            val verificationToken = saveVerificationToken(user.getEmail(), token, TokenTypes.EMAIL_CHANGE.value, request.newEmail, saga.id)

            sagaManager.recordSagaStep(
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

            authEventProducer.sendMailRequestedEvent(
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

            sagaManager.recordSagaStep(
                sagaId = saga.id,
                stepName = SagaStepNames.CREATE_MAIL_EVENT,
                status = SagaStepStatus.COMPLETED,
            )

            sagaManager.awaitResponse(saga.id)
        } catch (e: Exception) {
            logger.error("Email change request failed for {}: {}", email, e.message, e)
            throw e
        }
    }

    @Transactional
    fun confirmEmailChange(token: String) {
        val verificationToken =
            verificationTokenRepository
                .findByToken(token)
                .filter { it.tokenType == TokenTypes.EMAIL_CHANGE.value }
                .orElseThrow { ApiException(INVALID_TOKEN, HttpStatus.BAD_REQUEST) }

        if (verificationToken.used || verificationToken.expiryDate.isBefore(LocalDateTime.now())) {
            throw ApiException(TOKEN_EXPIRED_OR_USED, HttpStatus.BAD_REQUEST)
        }

        val user =
            userRepository
                .findByEmail(verificationToken.username)
                .orElseThrow { ApiException(USER_NOT_FOUND, HttpStatus.NOT_FOUND) }

        val newEmail =
            verificationToken.additionalData
                ?: throw ApiException(MISSING_NEW_EMAIL_DATA, HttpStatus.INTERNAL_SERVER_ERROR)

        val originalEmail = user.getEmail()
        user.setEmail(newEmail)
        userRepository.save(user)

        verificationToken.used = true
        verificationTokenRepository.save(verificationToken)

        verificationToken.sagaId?.let { sagaId ->
            sagaManager.recordSagaStep(
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
            sagaManager.completeSaga(sagaId)
        }
    }

    @Transactional
    fun changePassword(
        email: String,
        request: ChangePasswordRequestDto,
    ) {
        val user =
            userRepository
                .findByEmail(email)
                .orElseThrow { ApiException(USER_NOT_FOUND, HttpStatus.NOT_FOUND) }

        val saga =
            sagaManager.startSaga(
                sagaType = SagaTypes.PASSWORD_CHANGE,
                payload =
                    mapOf(
                        "email" to email,
                        "userId" to user.id!!,
                    ),
            )

        try {
            if (!passwordEncoder.matches(request.currentPassword, user.password)) {
                throw ApiException(INVALID_CURRENT_PASSWORD, HttpStatus.UNAUTHORIZED)
            }

            sagaManager.recordSagaStep(
                sagaId = saga.id,
                stepName = SagaStepNames.VERIFY_CURRENT_PASSWORD,
                status = SagaStepStatus.COMPLETED,
            )

            val token = UUID.randomUUID().toString()
            val verificationToken = saveVerificationToken(user.getEmail(), token, TokenTypes.PASSWORD_CHANGE_REQUEST.value, null, saga.id)

            sagaManager.recordSagaStep(
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

            authEventProducer.sendMailRequestedEvent(
                MailRequestedEvent(
                    to = user.getEmail(),
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

            sagaManager.recordSagaStep(
                sagaId = saga.id,
                stepName = SagaStepNames.CREATE_MAIL_EVENT,
                status = SagaStepStatus.COMPLETED,
            )

            sagaManager.awaitResponse(saga.id)
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
            verificationTokenRepository
                .findByToken(token)
                .filter { it.tokenType == TokenTypes.PASSWORD_CHANGE_REQUEST.value }
                .orElseThrow { ApiException(INVALID_TOKEN, HttpStatus.BAD_REQUEST) }

        if (verificationToken.used || verificationToken.expiryDate.isBefore(LocalDateTime.now())) {
            throw ApiException(TOKEN_EXPIRED_OR_USED, HttpStatus.BAD_REQUEST)
        }

        val user =
            userRepository
                .findByEmail(verificationToken.username)
                .orElseThrow { ApiException(USER_NOT_FOUND, HttpStatus.NOT_FOUND) }

        val originalPasswordHash = user.password
        val encodedPassword =
            passwordEncoder.encode(newPassword)
                ?: throw ApiException(PASSWORD_ENCODING_FAILED, HttpStatus.INTERNAL_SERVER_ERROR)
        user.setPassword(encodedPassword)
        userRepository.save(user)

        verificationToken.used = true
        verificationTokenRepository.save(verificationToken)

        verificationToken.sagaId?.let { sagaId ->
            sagaManager.recordSagaStep(
                sagaId = sagaId,
                stepName = SagaStepNames.UPDATE_PASSWORD,
                status = SagaStepStatus.COMPLETED,
                payload =
                    mapOf(
                        "userId" to user.id!!,
                        "originalPasswordHash" to originalPasswordHash,
                    ),
            )
            sagaManager.completeSaga(sagaId)
        }
    }

    @Transactional
    fun setNewPassword(
        tokenId: Long,
        request: ResetPasswordRequestDto,
    ) {
        val verificationToken =
            verificationTokenRepository
                .findById(tokenId)
                .orElseThrow { ApiException(INVALID_TOKEN_ID, HttpStatus.BAD_REQUEST) }

        if (verificationToken.token != request.confirmationCode) {
            throw ApiException(INVALID_CONFIRMATION_CODE, HttpStatus.BAD_REQUEST)
        }

        if (verificationToken.used || verificationToken.expiryDate.isBefore(LocalDateTime.now())) {
            throw ApiException(TOKEN_EXPIRED_OR_USED, HttpStatus.BAD_REQUEST)
        }

        val user =
            userRepository
                .findByEmail(verificationToken.username)
                .orElseThrow { ApiException(USER_NOT_FOUND, HttpStatus.NOT_FOUND) }

        val encodedPassword =
            passwordEncoder.encode(request.newPassword)
                ?: throw ApiException(PASSWORD_ENCODING_FAILED, HttpStatus.INTERNAL_SERVER_ERROR)
        user.setPassword(encodedPassword)
        userRepository.save(user)

        verificationToken.used = true
        verificationTokenRepository.save(verificationToken)
    }

    @Transactional(readOnly = true)
    fun getActiveSessions(username: String): List<SessionDto> =
        refreshTokenRepository
            .findByUsername(username)
            .filter { !it.revoked && it.expiryDate.isAfter(Instant.now()) }
            .map {
                SessionDto(
                    id = it.id,
                    deviceInfo = it.deviceInfo,
                    createdAt = it.createdAt,
                    expiryDate = it.expiryDate,
                    isCurrentSession = false, // Note: identifying the current session would require a token check
                )
            }

    @Transactional
    fun revokeSession(
        sessionId: Long,
        username: String,
    ): Boolean {
        val token =
            refreshTokenRepository
                .findById(sessionId)
                .filter { it.username == username }
                .orElse(null) ?: return false

        token.revoked = true
        refreshTokenRepository.save(token)
        return true
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

    private fun addRefreshTokenCookie(
        response: HttpServletResponse,
        token: String,
    ) {
        val cookie = Cookie(jwtService.getRefreshTokenCookieNameFromConfig(), token)
        cookie.isHttpOnly = true
        cookie.path = "/"
        cookie.maxAge = (jwtService.getRefreshTokenExpirationTime() / MILLISECONDS_IN_SECOND).toInt()
        response.addCookie(cookie)
    }

    private fun deleteRefreshTokenCookie(response: HttpServletResponse) {
        val cookie = Cookie(jwtService.getRefreshTokenCookieNameFromConfig(), null)
        cookie.isHttpOnly = true
        cookie.path = "/"
        cookie.maxAge = 0
        response.addCookie(cookie)
    }

    private fun extractRefreshTokenFromCookies(request: HttpServletRequest): String? =
        request.cookies
            ?.find {
                it.name == jwtService.getRefreshTokenCookieNameFromConfig()
            }?.value
}
