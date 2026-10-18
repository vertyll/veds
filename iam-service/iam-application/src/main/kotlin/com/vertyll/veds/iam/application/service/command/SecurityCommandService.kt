package com.vertyll.veds.iam.application.service.command

import com.vertyll.veds.iam.application.port.inbound.command.SecurityCommandUseCase
import com.vertyll.veds.iam.application.port.outbound.IdentityProviderPort
import java.util.UUID

class SecurityCommandService(
    private val identityProvider: IdentityProviderPort,
) : SecurityCommandUseCase {
    override fun keepLanguage(
        keycloakId: UUID,
        language: String,
    ) {
        identityProvider.setLanguage(keycloakId, language)
    }
}
