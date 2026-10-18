package com.vertyll.veds.iam.application.port.inbound.query

import com.vertyll.veds.iam.application.dto.SecuritySettingsResponse
import java.util.UUID

@Suppress("kotlin:S6517")
interface SecurityQueryUseCase {
    fun getSecuritySettings(keycloakId: UUID): SecuritySettingsResponse
}
