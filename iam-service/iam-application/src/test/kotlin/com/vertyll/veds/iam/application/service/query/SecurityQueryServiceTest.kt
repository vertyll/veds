package com.vertyll.veds.iam.application.service.query

import com.vertyll.veds.iam.application.FakeIdentityProvider
import org.junit.jupiter.api.Test
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

internal class SecurityQueryServiceTest {
    private val identityProvider = FakeIdentityProvider()
    private val service = SecurityQueryService(identityProvider)
    private val keycloakId = UUID.fromString("0192a4f0-0000-7000-8000-000000000001")

    @Test
    fun `without a one-time password the second factor is off and names no credential`() {
        val settings = service.getSecuritySettings(keycloakId)

        assertFalse(settings.twoFactorEnabled)
        assertNull(settings.twoFactorCredentialId)
    }

    @Test
    fun `a one-time password turns the second factor on and names the credential Keycloak removes`() {
        identityProvider.credentials["otp"] = "otp-1"

        val settings = service.getSecuritySettings(keycloakId)

        assertTrue(settings.twoFactorEnabled)
        assertEquals("otp-1", settings.twoFactorCredentialId)
        assertEquals(listOf("otp", "password"), settings.configuredFactors)
    }
}
