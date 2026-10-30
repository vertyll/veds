package com.vertyll.veds.template.infrastructure.web

import com.vertyll.veds.template.infrastructure.IntegrationTestBase
import org.junit.jupiter.api.Test
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@AutoConfigureMockMvc
internal class TemplateApiIntegrationTest(
    private val mockMvc: MockMvc,
) : IntegrationTestBase() {
    @Test
    fun `the health endpoint is open and creating a template needs a token`() {
        mockMvc.perform(get("/actuator/health")).andExpect(status().isOk)

        mockMvc
            .perform(post("/template").contentType(MediaType.APPLICATION_JSON).content(VALID_REQUEST))
            .andExpect(status().isUnauthorized)
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
    }

    @Test
    fun `a signed-in caller creates a template that comes back processed`() {
        mockMvc
            .perform(post("/template").with(jwt()).contentType(MediaType.APPLICATION_JSON).content(VALID_REQUEST))
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.name").value("Welcome"))
            .andExpect(jsonPath("$.status").value("PROCESSED"))
            .andExpect(jsonPath("$.id").isNotEmpty)
    }

    @Test
    fun `a template without a name is refused`() {
        mockMvc
            .perform(
                post("/template")
                    .with(jwt())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"name":"","payload":"body"}"""),
            ).andExpect(status().isBadRequest)
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
    }

    private companion object {
        const val VALID_REQUEST = """{"name":"Welcome","payload":"body"}"""
    }
}
