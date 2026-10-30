package com.vertyll.veds.file.infrastructure.web

import com.vertyll.veds.file.infrastructure.IntegrationTestBase
import org.junit.jupiter.api.Test
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.util.UUID

@AutoConfigureMockMvc
internal class FileApiIntegrationTest(
    private val mockMvc: MockMvc,
) : IntegrationTestBase() {
    private fun caller() =
        jwt().jwt {
            it.subject(UUID.randomUUID().toString()).claim("email", "caller@example.com")
        }

    @Test
    fun `the health endpoint is open and a file needs a token`() {
        mockMvc.perform(get("/actuator/health")).andExpect(status().isOk)

        mockMvc.perform(get("/files/{id}", UUID.randomUUID())).andExpect(status().isUnauthorized)
    }

    @Test
    fun `a file that does not exist answers with a problem document`() {
        mockMvc
            .perform(get("/files/{id}", UUID.randomUUID()).with(caller()))
            .andExpect(status().isNotFound)
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath("$.code").value("file.not_found"))
    }

    @Test
    fun `a token whose subject is no identifier is refused`() {
        mockMvc
            .perform(get("/files/{id}", UUID.randomUUID()).with(jwt().jwt { it.subject("not-a-uuid") }))
            .andExpect(status().isForbidden)
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
    }
}
