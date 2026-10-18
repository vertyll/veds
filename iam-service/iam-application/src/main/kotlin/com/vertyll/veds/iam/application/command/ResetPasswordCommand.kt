package com.vertyll.veds.iam.application.command

data class ResetPasswordCommand(
    val newPassword: String,
    val confirmationCode: String,
)
