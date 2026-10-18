package com.vertyll.veds.mail.domain.model

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class SenderAddressTest {
    @Test
    fun `a configured address is kept as given`() {
        assertEquals("no-reply@veds.local", SenderAddress("no-reply@veds.local").value)
    }

    @Test
    fun `a blank address is refused`() {
        listOf("", " ", "\t").forEach { blank ->
            assertFailsWith<IllegalArgumentException> { SenderAddress(blank) }
        }
    }
}
