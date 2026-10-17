package com.vertyll.veds.iam.application.service.command

import com.vertyll.veds.iam.application.FakeIdentityProvider
import org.junit.jupiter.api.Test
import java.util.UUID
import kotlin.test.assertEquals

internal class SecurityCommandServiceTest {
    private val identityProvider = FakeIdentityProvider()
    private val service = SecurityCommandService(identityProvider)

    @Test
    fun `the language is stored on the caller's Keycloak account`() {
        val keycloakId = UUID.fromString("0192a4f0-0000-7000-8000-000000000001")

        service.keepLanguage(keycloakId, "en")

        assertEquals(listOf("setLanguage($keycloakId,en)"), identityProvider.calls)
    }
}
