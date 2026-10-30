package com.vertyll.veds.translation.infrastructure.web

import com.vertyll.veds.translation.infrastructure.IntegrationTestBase
import org.junit.jupiter.api.Test
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.header
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@AutoConfigureMockMvc
internal class TranslationCatalogueIntegrationTest(
    private val mockMvc: MockMvc,
) : IntegrationTestBase() {
    @Test
    fun `the languages are seeded at start-up, Polish the default`() {
        mockMvc
            .perform(get("/translations/languages"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[?(@.tag == 'pl')].isDefault").value(true))
            .andExpect(jsonPath("$[?(@.tag == 'en')].isDefault").value(false))
    }

    @Test
    fun `the service's own catalogue is served at start-up in each language, with no token`() {
        mockMvc
            .perform(get("/translations/{language}", "pl"))
            .andExpect(status().isOk)
            .andExpect(header().exists(HttpHeaders.ETAG))
            .andExpect(jsonPath("$.language").value("pl"))
            .andExpect(jsonPath("$.entries['common.access_denied']").value("Nie masz uprawnień do tej operacji."))
            .andExpect(jsonPath("$.entries['translation.export.column.key']").value("Klucz"))

        mockMvc
            .perform(get("/translations/{language}", "en"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.entries['common.access_denied']").value("You are not allowed to perform this action."))
    }

    @Test
    fun `a service registers its keys and they are served`() {
        val catalogue =
            """
            {
              "sourceService": "sample-service",
              "entries": [
                {"key": "sample.greeting", "description": "A greeting", "defaultValues": {"pl": "Cześć", "en": "Hello"}}
              ]
            }
            """.trimIndent()

        mockMvc
            .perform(
                post("/internal/translations/catalogue").contentType(MediaType.APPLICATION_JSON).content(catalogue),
            ).andExpect(status().isOk)

        mockMvc
            .perform(get("/translations/{language}", "pl"))
            .andExpect(jsonPath("$.entries['sample.greeting']").value("Cześć"))
    }

    @Test
    fun `the administration needs a token`() {
        mockMvc.perform(get("/admin/translations/keys")).andExpect(status().isUnauthorized)
    }
}
