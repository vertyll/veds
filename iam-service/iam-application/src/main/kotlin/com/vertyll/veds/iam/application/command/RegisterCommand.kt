package com.vertyll.veds.iam.application.command

data class RegisterCommand(
    val firstName: String,
    val lastName: String,
    val email: String,
    val password: String,
)
