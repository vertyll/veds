package com.vertyll.veds.iam.application.command

data class ChangeEmailCommand(
    val password: String,
    val newEmail: String,
)
