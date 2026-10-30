package com.vertyll.veds.iam.infrastructure.web

import com.jayway.jsonpath.JsonPath
import com.vertyll.veds.iam.infrastructure.IntegrationTestBase
import com.vertyll.veds.iam.infrastructure.RecordingIdentityProvider
import com.vertyll.veds.shared.messaging.kafka.persistence.outbox.OutboxRepository
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.Primary
import org.springframework.http.MediaType
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.ResultActions
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@AutoConfigureMockMvc
@Import(IamApiIntegrationTest.IdentityProvider::class)
internal class IamApiIntegrationTest(
    private val mockMvc: MockMvc,
    private val identityProvider: RecordingIdentityProvider,
    private val outbox: OutboxRepository,
) : IntegrationTestBase() {
    @TestConfiguration
    class IdentityProvider {
        @Bean
        @Primary
        fun recordingIdentityProvider() = RecordingIdentityProvider()
    }

    private val personId = UUID.randomUUID()
    private val personEmail = "$personId@example.com"

    private fun MockHttpServletRequestBuilder.asPerson() =
        with(
            jwt().jwt { token ->
                token
                    .subject(personId.toString())
                    .claim("email", personEmail)
                    .claim("given_name", "Ada")
                    .claim("family_name", "Lovelace")
            },
        )

    private fun MockHttpServletRequestBuilder.asAdmin() = with(jwt().authorities(SimpleGrantedAuthority("ROLE_ADMIN")))

    private fun MockHttpServletRequestBuilder.json(body: String) = contentType(MediaType.APPLICATION_JSON).content(body)

    private fun signIn(): ResultActions = mockMvc.perform(get("/auth/me").asPerson()).andExpect(status().isOk)

    private fun signedInUserId(): Any = JsonPath.read(signIn().andReturn().response.contentAsString, "$.id")

    private fun aRoleName() = "ROLE_${UUID.randomUUID().toString().take(8).uppercase().replace('-', '_')}"

    private fun createRole(name: String) =
        mockMvc
            .perform(
                post("/roles")
                    .asAdmin()
                    .json("""{"name":"$name","description":"A role","permissions":[],"scope":"GLOBAL"}"""),
            ).andExpect(status().isCreated)

    @Test
    fun `a first sign-in provisions the person and announces them`() {
        signIn()
            .andExpect(jsonPath("$.email").value(personEmail))
            .andExpect(jsonPath("$.firstName").value("Ada"))

        assertEquals(1, outbox.findAll().count { it.topic == "user-registered" && it.key == personId.toString() })
    }

    @Test
    fun `a second sign-in changes nothing`() {
        val first = signIn().andReturn().response.contentAsString

        val second = signIn().andReturn().response.contentAsString

        assertEquals(JsonPath.read<Any>(first, "$.id"), JsonPath.read<Any>(second, "$.id"))
    }

    @Test
    fun `a signed-in person reads their permissions`() {
        signIn()

        mockMvc.perform(get("/auth/me/permissions").asPerson()).andExpect(status().isOk)
    }

    @Test
    fun `a person edits their own profile and the change is announced`() {
        signIn()

        mockMvc
            .perform(put("/users/me").asPerson().json("""{"firstName":"Augusta","lastName":"King","address":"London"}"""))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.firstName").value("Augusta"))

        assertTrue(outbox.findAll().any { it.topic == "user-profile-updated" && it.key == personId.toString() })
    }

    @Test
    fun `an administrator finds a person by id and by address`() {
        val id = signedInUserId()

        mockMvc
            .perform(get("/users/{id}", id).asAdmin())
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.email").value(personEmail))
        mockMvc
            .perform(get("/users/email/{email}", personEmail).asAdmin())
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.firstName").value("Ada"))
        mockMvc.perform(get("/users").asAdmin()).andExpect(status().isOk)
    }

    @Test
    fun `an administrator edits someone else's profile`() {
        val id = signedInUserId()

        mockMvc
            .perform(put("/users/{id}", id).asAdmin().json("""{"firstName":"Augusta","lastName":"King"}"""))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.lastName").value("King"))
    }

    @Test
    fun `someone without the permission sees no users`() {
        mockMvc.perform(get("/users").asPerson()).andExpect(status().isForbidden)
    }

    @Test
    fun `a role is created, read, changed and deleted, in Keycloak too`() {
        val name = aRoleName()

        val id = JsonPath.read<Any>(createRole(name).andReturn().response.contentAsString, "$.id")

        mockMvc.perform(get("/roles/{id}", id).asAdmin()).andExpect(status().isOk)
        mockMvc
            .perform(get("/roles/name/{name}", name).asAdmin())
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.name").value(name))
        mockMvc.perform(get("/roles").asAdmin()).andExpect(status().isOk)
        mockMvc
            .perform(put("/roles/name/{name}", name).asAdmin().json("""{"description":"Changed","permissions":[]}"""))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.description").value("Changed"))
        mockMvc.perform(delete("/roles/name/{name}", name).asAdmin()).andExpect(status().is2xxSuccessful)

        mockMvc.perform(get("/roles/name/{name}", name).asAdmin()).andExpect(status().isNotFound)
        assertTrue("createRole $name" in identityProvider.calls)
        assertTrue("deleteRole $name" in identityProvider.calls)
    }

    @Test
    fun `a role is given to a person and taken back`() {
        val id = signedInUserId()
        val name = aRoleName()
        createRole(name)

        mockMvc
            .perform(post("/roles/user/{userId}/role/{role}", id, name).asAdmin())
            .andExpect(status().is2xxSuccessful)
        mockMvc
            .perform(get("/roles/user/{userId}", id).asAdmin())
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[?(@.name == '$name')]").isNotEmpty)
        mockMvc
            .perform(delete("/roles/user/{userId}/role/{role}", id, name).asAdmin())
            .andExpect(status().is2xxSuccessful)

        assertTrue("assignRole $name" in identityProvider.calls)
        assertTrue("removeRole $name" in identityProvider.calls)
    }

    @Test
    fun `a catalogue with an unknown scope is refused`() {
        mockMvc
            .perform(
                post("/internal/authz/catalogue").json(
                    """{"module":"odd","permissions":[{"name":"ODD_READ","scope":"NO_SUCH_SCOPE"}],"stockRoles":[]}""",
                ),
            ).andExpect(status().isBadRequest)
    }
}
