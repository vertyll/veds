package com.vertyll.veds.iam.application.controller

import com.vertyll.veds.iam.domain.dto.ChangeEmailRequestDto
import com.vertyll.veds.iam.domain.dto.ChangePasswordRequestDto
import com.vertyll.veds.iam.domain.dto.ConfirmPasswordChangeDto
import com.vertyll.veds.iam.domain.dto.RegisterRequestDto
import com.vertyll.veds.iam.domain.dto.ResetPasswordRequestDto
import com.vertyll.veds.iam.domain.service.AuthService
import com.vertyll.veds.iam.infrastructure.response.ApiResponse
import com.vertyll.veds.sharedinfrastructure.config.SharedConfigProperties
import com.vertyll.veds.sharedinfrastructure.util.KeycloakJwtUtils
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/auth")
@Tag(name = "Authentication", description = "Authentication management APIs")
class AuthController(
    private val authService: AuthService,
    private val sharedConfigProperties: SharedConfigProperties,
) {
    private companion object {
        private const val ACCOUNT_ACTIVATED_SUCCESSFULLY = "Account activated successfully"
        private const val USER_REGISTERED_SUCCESSFULLY = "User registered successfully"
        private const val MESSAGE_NOT_AUTHENTICATED = "Not authenticated"
        private const val ACTIVATION_EMAIL_SENT = "Activation email sent. Please check your inbox."
        private const val PASSWORD_RESET_INSTRUCTIONS_SENT_TO_EMAIL = "Password reset instructions sent to email"
        private const val PASSWORD_RESET_SUCCESSFULLY = "Password reset successfully"
        private const val EMAIL_CHANGE_INSTRUCTIONS_SEND_TO_EMAIL = "Email change instructions sent to email"
        private const val EMAIL_CHANGED_SUCCESSFULLY = "Email changed successfully"
        private const val PASSWORD_CHANGE_CONFIRMATION_SENT = "Password change confirmation sent to email"
        private const val PASSWORD_CHANGED_SUCCESSFULLY = "Password changed successfully"
        private const val USER_DETAILS_RETRIEVED_SUCCESSFULLY = "User details retrieved successfully"
        private const val PERMISSIONS_RETRIEVED_SUCCESSFULLY = "Permissions retrieved successfully"
    }

    @PostMapping("/register")
    @Operation(summary = "Register new user")
    fun register(
        @RequestBody @Valid
        request: RegisterRequestDto,
    ): ResponseEntity<ApiResponse<Any>> {
        authService.register(request)
        return ApiResponse.buildResponse(
            data = null,
            message = USER_REGISTERED_SUCCESSFULLY,
            status = HttpStatus.OK,
        )
    }

    @PostMapping("/activate")
    @Operation(summary = "Activate user account with activation code")
    fun activateAccount(
        @RequestParam token: String,
    ): ResponseEntity<ApiResponse<Any>> {
        authService.activateAccount(token)
        return ApiResponse.buildResponse(
            data = null,
            message = ACCOUNT_ACTIVATED_SUCCESSFULLY,
            status = HttpStatus.OK,
        )
    }

    @PostMapping("/resend-activation")
    @Operation(summary = "Resend activation email")
    fun resendActivationEmail(
        @RequestParam email: String,
    ): ResponseEntity<ApiResponse<Any>> {
        authService.resendActivationEmail(email)
        return ApiResponse.buildResponse(
            data = null,
            message = ACTIVATION_EMAIL_SENT,
            status = HttpStatus.OK,
        )
    }

    @PostMapping("/reset-password-request")
    @Operation(summary = "Request password reset for a forgotten password")
    fun requestPasswordReset(
        @RequestParam email: String,
    ): ResponseEntity<ApiResponse<Any>> {
        authService.sendPasswordResetRequest(email)
        return ApiResponse.buildResponse(
            data = null,
            message = PASSWORD_RESET_INSTRUCTIONS_SENT_TO_EMAIL,
            status = HttpStatus.OK,
        )
    }

    @PostMapping("/confirm-reset-password")
    @Operation(summary = "Reset password using reset token")
    fun resetPassword(
        @RequestParam token: String,
        @RequestBody @Valid
        request: ResetPasswordRequestDto,
    ): ResponseEntity<ApiResponse<Any>> {
        authService.resetPassword(token, request)
        return ApiResponse.buildResponse(
            data = null,
            message = PASSWORD_RESET_SUCCESSFULLY,
            status = HttpStatus.OK,
        )
    }

    @PostMapping("/change-email-request")
    @Operation(summary = "Request email change")
    fun requestEmailChange(
        @AuthenticationPrincipal jwt: Jwt,
        @RequestBody @Valid
        request: ChangeEmailRequestDto,
    ): ResponseEntity<ApiResponse<Any>> {
        val email = jwt.getClaimAsString("email")
        authService.requestEmailChange(email, request)
        return ApiResponse.buildResponse(
            data = null,
            message = EMAIL_CHANGE_INSTRUCTIONS_SEND_TO_EMAIL,
            status = HttpStatus.OK,
        )
    }

    @PostMapping("/confirm-email-change")
    @Operation(summary = "Confirm email change using token")
    fun confirmEmailChange(
        @RequestParam token: String,
    ): ResponseEntity<ApiResponse<Any>> {
        authService.confirmEmailChange(token)
        return ApiResponse.buildResponse(
            data = null,
            message = EMAIL_CHANGED_SUCCESSFULLY,
            status = HttpStatus.OK,
        )
    }

    @PostMapping("/change-password-request")
    @Operation(summary = "Request to change password")
    fun changePassword(
        @AuthenticationPrincipal jwt: Jwt,
        @RequestBody @Valid
        request: ChangePasswordRequestDto,
    ): ResponseEntity<ApiResponse<Any>> {
        val email = jwt.getClaimAsString("email")
        authService.changePassword(email, request)
        return ApiResponse.buildResponse(
            data = null,
            message = PASSWORD_CHANGE_CONFIRMATION_SENT,
            status = HttpStatus.OK,
        )
    }

    @PostMapping("/confirm-password-change")
    @Operation(summary = "Confirm password change using token and new password")
    fun confirmPasswordChange(
        @RequestParam token: String,
        @RequestBody @Valid
        request: ConfirmPasswordChangeDto,
    ): ResponseEntity<ApiResponse<Any>> {
        authService.confirmPasswordChange(token, request.newPassword)
        return ApiResponse.buildResponse(
            data = null,
            message = PASSWORD_CHANGED_SUCCESSFULLY,
            status = HttpStatus.OK,
        )
    }

    @PostMapping("/set-new-password")
    @Operation(summary = "Set new password after token verification (second step of password change)")
    fun setNewPassword(
        @RequestParam tokenId: Long,
        @RequestBody @Valid
        request: ResetPasswordRequestDto,
    ): ResponseEntity<ApiResponse<Any>> {
        authService.setNewPassword(tokenId, request)
        return ApiResponse.buildResponse(
            data = null,
            message = PASSWORD_CHANGED_SUCCESSFULLY,
            status = HttpStatus.OK,
        )
    }

    @GetMapping("/me")
    @Operation(summary = "Get current authenticated user details from JWT")
    fun getCurrentUser(
        @AuthenticationPrincipal jwt: Jwt?,
    ): ResponseEntity<ApiResponse<Map<String, Any>>> {
        if (jwt == null) {
            return ApiResponse.buildResponse(
                data = emptyMap(),
                message = MESSAGE_NOT_AUTHENTICATED,
                status = HttpStatus.UNAUTHORIZED,
            )
        }

        val roles = KeycloakJwtUtils.extractRoles(jwt, sharedConfigProperties.keycloak.rolesClaimPath)

        val userInfo =
            mapOf(
                "sub" to jwt.subject,
                "email" to (jwt.getClaimAsString("email") ?: ""),
                "roles" to roles,
            )

        return ApiResponse.buildResponse(
            data = userInfo,
            message = USER_DETAILS_RETRIEVED_SUCCESSFULLY,
            status = HttpStatus.OK,
        )
    }

    @GetMapping("/me/permissions")
    @Operation(summary = "Get current user permissions from local database")
    fun getCurrentUserPermissions(
        @AuthenticationPrincipal jwt: Jwt?,
    ): ResponseEntity<ApiResponse<List<String>>> {
        if (jwt == null) {
            return ApiResponse.buildResponse(
                data = emptyList(),
                message = MESSAGE_NOT_AUTHENTICATED,
                status = HttpStatus.UNAUTHORIZED,
            )
        }

        val keycloakId = UUID.fromString(jwt.subject)
        val permissions = authService.getUserPermissions(keycloakId)

        return ApiResponse.buildResponse(
            data = permissions,
            message = PERMISSIONS_RETRIEVED_SUCCESSFULLY,
            status = HttpStatus.OK,
        )
    }
}
