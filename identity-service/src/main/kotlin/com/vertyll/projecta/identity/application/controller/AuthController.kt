package com.vertyll.projecta.identity.application.controller

import com.vertyll.projecta.identity.domain.dto.AuthRequestDto
import com.vertyll.projecta.identity.domain.dto.AuthResponseDto
import com.vertyll.projecta.identity.domain.dto.ChangeEmailRequestDto
import com.vertyll.projecta.identity.domain.dto.ChangePasswordRequestDto
import com.vertyll.projecta.identity.domain.dto.ConfirmPasswordChangeDto
import com.vertyll.projecta.identity.domain.dto.RegisterRequestDto
import com.vertyll.projecta.identity.domain.dto.ResetPasswordRequestDto
import com.vertyll.projecta.identity.domain.dto.SessionDto
import com.vertyll.projecta.identity.domain.service.AuthService
import com.vertyll.projecta.identity.infrastructure.response.ApiResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/auth")
@Tag(name = "Authentication", description = "Authentication management APIs")
class AuthController(
    private val authService: AuthService,
) {
    private companion object {
        private const val ACCOUNT_ACTIVATED_SUCCESSFULLY = "Account activated successfully"
        private const val USER_REGISTERED_SUCCESSFULLY = "User registered successfully"
        private const val MESSAGE_NOT_AUTHENTICATED = "Not authenticated"
        private const val ACTIVATION_EMAIL_SENT = "Activation email sent. Please check your inbox."
        private const val AUTHENTICATION_SUCCESSFUL = "Authentication successful"
        private const val TOKEN_REFRESHED_SUCCESSFULLY = "Token refreshed successfully"
        private const val LOGGED_OUT_SUCCESSFULLY = "Logged out successfully"
        private const val PASSWORD_RESET_INSTRUCTIONS_SENT_TO_EMAIL = "Password reset instructions sent to email"
        private const val PASSWORD_RESET_SUCCESSFULLY = "Password reset successfully"
        private const val EMAIL_CHANGE_INSTRUCTIONS_SEND_TO_EMAIL = "Email change instructions sent to email"
        private const val EMAIL_CHANGED_SUCCESSFULLY = "Email changed successfully"
        private const val PASSWORD_CHANGE_CONFIRMATION_SENT = "Password change confirmation sent to email"
        private const val PASSWORD_CHANGED_SUCCESSFULLY = "Password changed successfully"
        private const val USER_DETAILS_RETRIEVED_SUCCESSFULLY = "User details retrieved successfully"
        private const val ACTIVE_SESSIONS_RETRIEVED_SUCCESSFULLY = "Active sessions retrieved successfully"
        private const val SESSION_REVOKED_SUCCESSFULLY = "Session revoked successfully"
        private const val FAILED_TO_REVOKE_SESSION = "Failed to revoke session"
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

    @PostMapping("/authenticate")
    @Operation(summary = "Authenticate user and get token")
    fun authenticate(
        @RequestBody @Valid
        request: AuthRequestDto,
        response: HttpServletResponse,
        httpRequest: HttpServletRequest,
    ): ResponseEntity<ApiResponse<AuthResponseDto>> {
        val userAgent = httpRequest.getHeader("User-Agent")
        val requestWithUserAgent = request.copy(userAgent = userAgent)

        val authResponse = authService.authenticate(requestWithUserAgent, response)
        return ApiResponse.buildResponse(
            data = authResponse,
            message = AUTHENTICATION_SUCCESSFUL,
            status = HttpStatus.OK,
        )
    }

    @PostMapping("/refresh-token")
    @Operation(summary = "Refresh access token using refresh token cookie")
    fun refreshToken(request: HttpServletRequest): ResponseEntity<ApiResponse<AuthResponseDto>> {
        val authResponse = authService.refreshToken(request)
        return ApiResponse.buildResponse(
            data = authResponse,
            message = TOKEN_REFRESHED_SUCCESSFULLY,
            status = HttpStatus.OK,
        )
    }

    @PostMapping("/logout")
    @Operation(summary = "Logout from current session")
    fun logout(
        request: HttpServletRequest,
        response: HttpServletResponse,
    ): ResponseEntity<ApiResponse<Any>> {
        authService.logout(request, response)
        return ApiResponse.buildResponse(
            data = null,
            message = LOGGED_OUT_SUCCESSFULLY,
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
        @RequestParam email: String,
        @RequestBody @Valid
        request: ChangeEmailRequestDto,
    ): ResponseEntity<ApiResponse<Any>> {
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
        @RequestParam email: String,
        @RequestBody @Valid
        request: ChangePasswordRequestDto,
    ): ResponseEntity<ApiResponse<Any>> {
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
    @Operation(summary = "Get current authenticated user details")
    fun getCurrentUser(
        @AuthenticationPrincipal userDetails: UserDetails?,
    ): ResponseEntity<ApiResponse<Map<String, Any>>> {
        if (userDetails == null) {
            return ApiResponse.buildResponse(
                data = emptyMap(),
                message = MESSAGE_NOT_AUTHENTICATED,
                status = HttpStatus.UNAUTHORIZED,
            )
        }

        val userInfo =
            mapOf(
                "username" to userDetails.username,
                "authorities" to userDetails.authorities.map { it.authority },
                "accountNonExpired" to userDetails.isAccountNonExpired,
                "accountNonLocked" to userDetails.isAccountNonLocked,
                "credentialsNonExpired" to userDetails.isCredentialsNonExpired,
                "enabled" to userDetails.isEnabled,
            )

        return ApiResponse.buildResponse(
            data = userInfo,
            message = USER_DETAILS_RETRIEVED_SUCCESSFULLY,
            status = HttpStatus.OK,
        )
    }

    @GetMapping("/sessions")
    @Operation(summary = "Get all active sessions for the current user")
    fun getSessions(
        @AuthenticationPrincipal userDetails: UserDetails?,
    ): ResponseEntity<ApiResponse<List<SessionDto>>> {
        if (userDetails == null) {
            return ApiResponse.buildResponse(
                data = emptyList(),
                message = MESSAGE_NOT_AUTHENTICATED,
                status = HttpStatus.UNAUTHORIZED,
            )
        }

        val sessions = authService.getActiveSessions(userDetails.username)
        return ApiResponse.buildResponse(
            data = sessions,
            message = ACTIVE_SESSIONS_RETRIEVED_SUCCESSFULLY,
            status = HttpStatus.OK,
        )
    }

    @PostMapping("/sessions/{sessionId}/revoke")
    @Operation(summary = "Revoke a specific session")
    fun revokeSession(
        @PathVariable sessionId: Long,
        @AuthenticationPrincipal userDetails: UserDetails?,
    ): ResponseEntity<ApiResponse<Any>> {
        if (userDetails == null) {
            return ApiResponse.buildResponse(
                data = null,
                message = MESSAGE_NOT_AUTHENTICATED,
                status = HttpStatus.UNAUTHORIZED,
            )
        }

        val success = authService.revokeSession(sessionId, userDetails.username)
        return if (success) {
            ApiResponse.buildResponse(
                data = null,
                message = SESSION_REVOKED_SUCCESSFULLY,
                status = HttpStatus.OK,
            )
        } else {
            ApiResponse.buildResponse(
                data = null,
                message = FAILED_TO_REVOKE_SESSION,
                status = HttpStatus.BAD_REQUEST,
            )
        }
    }
}
