package com.vertyll.veds.iam.application.service.query

import com.vertyll.veds.iam.application.dto.SecuritySettingsResponse
import com.vertyll.veds.iam.application.port.inbound.query.SecurityQueryUseCase
import com.vertyll.veds.iam.application.port.outbound.IdentityProviderPort
import java.util.UUID

class SecurityQueryService(
    private val identityProvider: IdentityProviderPort,
) : SecurityQueryUseCase {
    override fun getSecuritySettings(keycloakId: UUID): SecuritySettingsResponse {
        val factors = identityProvider.credentialTypes(keycloakId)

        return SecuritySettingsResponse(
            twoFactorEnabled = OTP_CREDENTIAL in factors,
            configuredFactors = factors.sorted(),
            twoFactorCredentialId = identityProvider.credentialId(keycloakId, OTP_CREDENTIAL),
        )
    }

    private companion object {
        const val OTP_CREDENTIAL = "otp"
    }
}
