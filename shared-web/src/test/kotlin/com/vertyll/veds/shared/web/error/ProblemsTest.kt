package com.vertyll.veds.shared.web.error

import org.springframework.http.HttpStatus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * The wire shape of a refusal is a contract with every client, so it is pinned here
 * rather than left to whichever service happens to be read first.
 */
class ProblemsTest {
    @Test
    fun `a refusal names the problem by its catalogue key`() {
        val problem = Problems.of(HttpStatus.NOT_FOUND, "task.not_found", instance = "/tasks/42")

        assertEquals(404, problem.status)
        assertEquals("urn:veds:error:task.not_found", problem.type.toString())
        assertEquals("Not Found", problem.title)
        assertEquals("/tasks/42", problem.instance.toString())
        assertEquals("task.not_found", problem.properties?.get("code"))
    }

    /** The prose lives in translation-service, so `detail` would only ever be a key pretending to be a sentence. */
    @Test
    fun `a refusal carries no detail`() {
        assertNull(Problems.of(HttpStatus.NOT_FOUND, "task.not_found").detail)
    }

    @Test
    fun `extension members ride along, and none are invented when there are none`() {
        val withParams =
            Problems.of(
                HttpStatus.NOT_FOUND,
                "task.not_found",
                properties = mapOf("params" to mapOf("id" to "42")),
            )
        assertEquals(mapOf("id" to "42"), withParams.properties?.get("params"))

        assertNull(Problems.of(HttpStatus.NOT_FOUND, "task.not_found").properties?.get("params"))
    }

    @Test
    fun `an instance is omitted rather than guessed when the caller has none`() {
        assertNull(Problems.of(HttpStatus.NOT_FOUND, "task.not_found").instance)
    }
}
