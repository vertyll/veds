package com.vertyll.veds.iam.application.port.inbound.command

import java.util.UUID

interface SecurityCommandUseCase {
    /**
     * Stores the language on the caller's Keycloak account, which Keycloak's pages show a signed-in person in, so an
     * account action opens in the language FastDo shows.
     */
    fun keepLanguage(
        keycloakId: UUID,
        language: String,
    )
}
