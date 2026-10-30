package com.vertyll.veds.iam.infrastructure.web.security

import com.vertyll.veds.iam.domain.error.IamError
import com.vertyll.veds.sharederror.ApiException
import org.junit.jupiter.api.Test
import org.springframework.security.oauth2.jwt.Jwt
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs

internal class CurrentUserTest {
    @Test
    fun `a subject that is no identifier is refused, and keeps why`() {
        val jwt =
            Jwt
                .withTokenValue("token")
                .header("alg", "none")
                .subject("not-a-uuid")
                .build()

        val failure = assertFailsWith<ApiException> { CurrentUser.keycloakIdOf(jwt) }

        assertEquals(IamError.TOKEN_CLAIM_MISSING, failure.error)
        assertIs<IllegalArgumentException>(failure.cause)
    }
}
