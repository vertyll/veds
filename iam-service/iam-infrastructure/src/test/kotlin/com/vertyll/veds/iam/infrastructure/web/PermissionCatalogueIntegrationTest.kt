package com.vertyll.veds.iam.infrastructure.web

import com.vertyll.veds.iam.application.port.inbound.query.PermissionQueryUseCase
import com.vertyll.veds.iam.infrastructure.IntegrationTestBase
import org.junit.jupiter.api.Test
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import kotlin.test.assertEquals

@AutoConfigureMockMvc
internal class PermissionCatalogueIntegrationTest(
    private val mockMvc: MockMvc,
    private val permissions: PermissionQueryUseCase,
) : IntegrationTestBase() {
    private fun register(body: String) =
        mockMvc.perform(
            post("/internal/authz/catalogue").contentType(MediaType.APPLICATION_JSON).content(body),
        )

    @Test
    fun `a module's permissions are stored, and show which stock roles grant them`() {
        register(
            """
            {
              "module": "sample",
              "permissions": [
                {"name": "SAMPLE_READ", "description": "Read samples", "scope": "PROJECT"},
                {"name": "SAMPLE_WRITE", "scope": "PROJECT"}
              ],
              "stockRoles": [
                {"name": "SAMPLE_REVIEWER", "scope": "PROJECT", "permissions": ["SAMPLE_READ"]},
                {"name": "SAMPLE_AUTHOR", "scope": "PROJECT", "permissions": ["SAMPLE_READ", "SAMPLE_WRITE"]}
              ]
            }
            """.trimIndent(),
        ).andExpect(status().isNoContent)

        val sample = permissions.getAllPermissions().filter { it.module == "sample" }

        assertEquals(listOf("SAMPLE_READ", "SAMPLE_WRITE"), sample.map { it.name })
        assertEquals(listOf("SAMPLE_AUTHOR", "SAMPLE_REVIEWER"), sample.first().grantedByRoles)
        assertEquals(listOf("SAMPLE_AUTHOR"), sample.last().grantedByRoles)
    }

    @Test
    fun `a catalogue without its module is refused`() {
        register("""{"module": "", "permissions": [], "stockRoles": []}""")
            .andExpect(status().isBadRequest)
    }
}
