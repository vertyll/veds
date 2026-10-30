package com.vertyll.veds.project.infrastructure.web

import com.jayway.jsonpath.JsonPath
import com.vertyll.veds.project.infrastructure.IntegrationTestBase
import org.junit.jupiter.api.Test
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.util.UUID

@AutoConfigureMockMvc
internal class ProjectApiIntegrationTest(
    private val mockMvc: MockMvc,
) : IntegrationTestBase() {
    private val ownerId = UUID.randomUUID()

    private fun MockHttpServletRequestBuilder.asOwner() =
        with(
            jwt().jwt { token ->
                token
                    .subject(ownerId.toString())
                    .claim("email", "owner@example.com")
                    .claim("given_name", "Ada")
                    .claim("family_name", "Lovelace")
            },
        ).header(LANGUAGE_HEADER, "en")

    private fun MockHttpServletRequestBuilder.asStranger() =
        with(
            jwt().jwt { token ->
                token
                    .subject(UUID.randomUUID().toString())
                    .claim("email", "stranger@example.com")
                    .claim("given_name", "Sam")
                    .claim("family_name", "Stranger")
            },
        ).header(LANGUAGE_HEADER, "en")

    private fun MockHttpServletRequestBuilder.json(body: String) = contentType(MediaType.APPLICATION_JSON).content(body)

    private fun createdId(request: MockHttpServletRequestBuilder): String =
        JsonPath.read(
            mockMvc
                .perform(request)
                .andExpect(status().isCreated)
                .andReturn()
                .response.contentAsString,
            "$.id",
        )

    private fun aProject(name: String = "Apollo"): String =
        createdId(post("/projects").asOwner().json("""{"name":"$name","description":"To the moon"}"""))

    private fun labelled(color: String) =
        """{"color":"$color","translations":[{"language":"en","name":"Label"},{"language":"pl","name":"Etykieta"}]}"""

    @Test
    fun `a created project is read back and listed for its owner`() {
        val projectId = aProject("Findable")

        mockMvc
            .perform(get("/projects/{id}", projectId).asOwner())
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.name").value("Findable"))
        mockMvc.perform(get("/projects/{id}/details", projectId).asOwner()).andExpect(status().isOk)
        mockMvc
            .perform(get("/projects").asOwner().param("searchTerm", "Findable"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.items[0].name").value("Findable"))
    }

    @Test
    fun `a project without a name is refused`() {
        mockMvc.perform(post("/projects").asOwner().json("""{"name":""}""")).andExpect(status().isBadRequest)
    }

    @Test
    fun `a project is renamed by its owner`() {
        val projectId = aProject()

        mockMvc
            .perform(put("/projects/{id}", projectId).asOwner().json("""{"name":"Artemis"}"""))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.name").value("Artemis"))
    }

    @Test
    fun `someone outside a private project cannot change it`() {
        val projectId = aProject()

        mockMvc
            .perform(put("/projects/{id}", projectId).asStranger().json("""{"name":"Hijacked"}"""))
            .andExpect(status().is4xxClientError)
    }

    @Test
    fun `an archived project leaves the active list`() {
        val projectId = aProject("Finished")

        mockMvc.perform(delete("/projects/{id}", projectId).asOwner()).andExpect(status().is2xxSuccessful)

        mockMvc
            .perform(get("/projects").asOwner().param("searchTerm", "Finished"))
            .andExpect(jsonPath("$.items").isEmpty)
    }

    @Test
    fun `a category is added, changed, listed and removed`() {
        val projectId = aProject()

        val categoryId =
            createdId(post("/projects/{id}/categories", projectId).asOwner().json(labelled("#ff0000")))
        mockMvc
            .perform(
                put("/projects/{id}/categories/{categoryId}", projectId, categoryId)
                    .asOwner()
                    .json(labelled("#00ff00")),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$.color").value("#00ff00"))
        mockMvc
            .perform(get("/projects/{id}/categories", projectId).asOwner())
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[?(@.color == '#00ff00')]").isNotEmpty)
        mockMvc
            .perform(delete("/projects/{id}/categories/{categoryId}", projectId, categoryId).asOwner())
            .andExpect(status().is2xxSuccessful)

        mockMvc
            .perform(get("/projects/{id}/categories", projectId).asOwner())
            .andExpect(jsonPath("$[?(@.color == '#00ff00')]").isEmpty)
    }

    @Test
    fun `a status is added, changed, listed and removed`() {
        val projectId = aProject()

        val statusId = createdId(post("/projects/{id}/statuses", projectId).asOwner().json(labelled("#111111")))
        mockMvc
            .perform(
                put("/projects/{id}/statuses/{statusId}", projectId, statusId)
                    .asOwner()
                    .json(labelled("#222222")),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$.color").value("#222222"))
        mockMvc
            .perform(get("/projects/{id}/statuses", projectId).asOwner())
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[?(@.color == '#222222')]").isNotEmpty)
        mockMvc
            .perform(delete("/projects/{id}/statuses/{statusId}", projectId, statusId).asOwner())
            .andExpect(status().is2xxSuccessful)

        mockMvc
            .perform(get("/projects/{id}/statuses", projectId).asOwner())
            .andExpect(jsonPath("$[?(@.color == '#222222')]").isEmpty)
    }

    @Test
    fun `the owner is a member and reads their permissions`() {
        val projectId = aProject()

        mockMvc
            .perform(get("/projects/{id}/users", projectId).asOwner())
            .andExpect(status().isOk)
            .andExpect(jsonPath("$").isNotEmpty)
        mockMvc
            .perform(get("/projects/{id}/users/me/permissions", projectId).asOwner())
            .andExpect(status().isOk)
            .andExpect(jsonPath("$").isNotEmpty)
    }

    private companion object {
        const val LANGUAGE_HEADER = "X-Lang"
    }
}
