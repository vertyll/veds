package com.vertyll.veds.shared.web.error

import org.springframework.http.HttpStatus
import org.springframework.http.ProblemDetail
import org.springframework.http.converter.json.ProblemDetailJacksonMixin
import tools.jackson.core.type.TypeReference
import tools.jackson.databind.json.JsonMapper
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * RFC 9457 puts extension members beside `type` and `status`, never inside a holder.
 *
 * They land there only because Spring registers [ProblemDetailJacksonMixin] on the
 * mapper — a plain `ObjectMapper` nests them under `properties` and emits a null
 * `detail`, which is not a problem document. Anything serializing a
 * [org.springframework.http.ProblemDetail] by hand, as the gateway's authentication
 * entry point does, has to use the container's mapper for the same reason.
 */
class ProblemSerialisationTest {
    private fun springMapper() =
        JsonMapper
            .builder()
            .addMixIn(ProblemDetail::class.java, ProblemDetailJacksonMixin::class.java)
            .build()

    @Test
    fun `extension members are serialised beside the standard ones`() {
        val json =
            springMapper().writeValueAsString(
                Problems.of(
                    HttpStatus.BAD_REQUEST,
                    "common.validation_failed",
                    instance = "/tasks",
                    properties = mapOf("fields" to mapOf("name" to "common.invalid_value")),
                ),
            )

        val parsed: Map<String, Any> = springMapper().readValue(json, object : TypeReference<Map<String, Any>>() {})

        assertEquals("urn:veds:error:common.validation_failed", parsed["type"])
        assertEquals("Bad Request", parsed["title"])
        assertEquals(400, parsed["status"])
        assertEquals("/tasks", parsed["instance"])
        assertEquals("common.validation_failed", parsed["code"])
        assertEquals(mapOf("name" to "common.invalid_value"), parsed["fields"])
        assertNull(parsed["properties"], "extensions must not nest under a holder")
        assertNull(parsed["detail"])
    }
}
