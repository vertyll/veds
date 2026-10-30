package com.vertyll.veds.translation.infrastructure.web.security

import com.vertyll.veds.sharederror.ApiException
import com.vertyll.veds.translation.domain.error.TranslationError
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

        val failure = assertFailsWith<ApiException> { CurrentUser.idOf(jwt) }

        assertEquals(TranslationError.TOKEN_CLAIM_MISSING, failure.error)
        assertIs<IllegalArgumentException>(failure.cause)
    }
}
