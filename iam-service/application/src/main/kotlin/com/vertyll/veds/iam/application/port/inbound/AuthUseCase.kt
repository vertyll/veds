package com.vertyll.veds.iam.application.port.inbound

import com.vertyll.veds.iam.application.dto.ChangeEmailRequest
import com.vertyll.veds.iam.application.dto.ChangePasswordRequest
import com.vertyll.veds.iam.application.dto.RegisterRequest
import com.vertyll.veds.iam.application.dto.ResetPasswordRequest
import java.util.UUID

interface AuthUseCase {
    fun register(request: RegisterRequest)

    fun activateAccount(token: String)

    fun resendActivationEmail(email: String)

    fun sendPasswordResetRequest(email: String)

    fun resetPassword(
        token: String,
        request: ResetPasswordRequest,
    )

    fun requestEmailChange(
        email: String,
        request: ChangeEmailRequest,
    )

    fun confirmEmailChange(token: String)

    fun changePassword(
        email: String,
        request: ChangePasswordRequest,
    )

    fun confirmPasswordChange(
        token: String,
        newPassword: String,
    )

    fun setNewPassword(
        tokenId: Long,
        request: ResetPasswordRequest,
    )

    fun getUserPermissions(keycloakId: UUID): List<String>
}
