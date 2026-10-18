package com.vertyll.veds.iam.application.port.inbound.query

import java.util.UUID

@Suppress("kotlin:S6517")
interface AuthQueryUseCase {
    fun getUserPermissions(keycloakId: UUID): List<String>
}
