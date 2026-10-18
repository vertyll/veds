package com.vertyll.veds.iam.application.dto

import java.util.UUID

data class AuthenticatedIdentity(
    val keycloakId: UUID,
    val email: String,
    val firstName: String,
    val lastName: String,
)
