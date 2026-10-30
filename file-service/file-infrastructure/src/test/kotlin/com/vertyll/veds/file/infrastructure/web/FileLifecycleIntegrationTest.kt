package com.vertyll.veds.file.infrastructure.web

import com.jayway.jsonpath.JsonPath
import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import com.vertyll.veds.file.application.port.outbound.ObjectStoragePort
import com.vertyll.veds.file.infrastructure.IntegrationTestBase
import com.vertyll.veds.shared.messaging.kafka.persistence.outbox.OutboxRepository
import org.junit.jupiter.api.Test
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.net.InetAddress
import java.net.InetSocketAddress
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** The file's way through the API, with a stand-in for the S3 store, which answers what the adapter asks of it. */
@AutoConfigureMockMvc
internal class FileLifecycleIntegrationTest(
    private val mockMvc: MockMvc,
    private val storage: ObjectStoragePort,
    private val outbox: OutboxRepository,
) : IntegrationTestBase() {
    private val ownerId = UUID.randomUUID()

    private fun owner() = jwt().jwt { it.subject(ownerId.toString()).claim("email", "owner@example.com") }

    private fun stranger() = jwt().jwt { it.subject(UUID.randomUUID().toString()).claim("email", "x@example.com") }

    private fun requestTicket(
        name: String = "report.pdf",
        contentType: String = "application/pdf",
        scope: String = "TASK_ATTACHMENT",
    ) = mockMvc.perform(
        post("/files/upload-ticket")
            .with(owner())
            .contentType(MediaType.APPLICATION_JSON)
            .content(
                """{"originalName":"$name","contentType":"$contentType","declaredSizeBytes":100,"scope":"$scope"}""",
            ),
    )

    private fun uploadedFile(): String {
        val fileId =
            JsonPath.read<String>(
                requestTicket()
                    .andExpect(status().isCreated)
                    .andReturn()
                    .response.contentAsString,
                "$.fileId",
            )
        objects[objectKeyOf(fileId)] = STORED_SIZE
        return fileId
    }

    private fun objectKeyOf(fileId: String): String = "task_attachment/$fileId.pdf"

    @Test
    fun `an upload ticket carries a signed address for the store`() {
        requestTicket()
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.uploadUrl").isNotEmpty)
            .andExpect(jsonPath("$.maxSizeBytes").isNumber)
    }

    @Test
    fun `an avatar that is no image is refused`() {
        requestTicket(scope = "USER_AVATAR").andExpect(status().is4xxClientError)
    }

    @Test
    fun `a confirmed upload takes its size from the store and is announced`() {
        val fileId = uploadedFile()

        mockMvc
            .perform(post("/files/{id}/confirm", fileId).with(owner()))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.sizeBytes").value(STORED_SIZE))

        assertEquals(1, outbox.findAll().count { it.topic == "file-confirmed" && it.key == fileId })
    }

    @Test
    fun `an upload that never reached the store cannot be confirmed`() {
        val fileId =
            JsonPath.read<String>(requestTicket().andReturn().response.contentAsString, "$.fileId")

        mockMvc.perform(post("/files/{id}/confirm", fileId).with(owner())).andExpect(status().is4xxClientError)
    }

    @Test
    fun `a confirmed file is attached, read and given a download ticket`() {
        val fileId = uploadedFile()
        mockMvc.perform(post("/files/{id}/confirm", fileId).with(owner()))
        val scopeId = UUID.randomUUID()

        mockMvc
            .perform(
                post("/files/{id}/attach", fileId)
                    .with(owner())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"scopeId":"$scopeId"}"""),
            ).andExpect(status().isOk)

        mockMvc
            .perform(get("/files/{id}", fileId).with(owner()))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.scopeId").value(scopeId.toString()))
        val ticket =
            mockMvc
                .perform(get("/files/{id}/download-ticket", fileId).with(owner()))
                .andExpect(status().isOk)
                .andReturn()
                .response
                .contentAsString
        assertContains(ticket, "report.pdf")
    }

    @Test
    fun `someone else's file does not exist for them`() {
        val fileId = uploadedFile()

        mockMvc.perform(post("/files/{id}/confirm", fileId).with(stranger())).andExpect(status().isNotFound)
    }

    @Test
    fun `a deleted file is announced`() {
        val fileId = uploadedFile()
        mockMvc.perform(post("/files/{id}/confirm", fileId).with(owner()))

        mockMvc.perform(delete("/files/{id}", fileId).with(owner())).andExpect(status().is2xxSuccessful)

        assertEquals(1, outbox.findAll().count { it.topic == "file-deleted" && it.key == fileId })
    }

    @Test
    fun `the store answers the size of an object, and nothing for a missing one`() {
        objects["sized/object"] = STORED_SIZE

        assertEquals(STORED_SIZE.toLong(), storage.sizeOf("sized/object"))
        assertNull(storage.sizeOf("missing/object"))
    }

    @Test
    fun `an object can be removed from the store`() {
        objects["doomed/object"] = STORED_SIZE

        storage.delete("doomed/object")

        assertFalse(objects.containsKey("doomed/object"))
        assertTrue(storage.sizeOf("doomed/object") == null)
    }

    companion object {
        private const val STORED_SIZE = 1234
        private const val OK = 200
        private const val NO_CONTENT = 204
        private const val NOT_FOUND = 404

        private val objects = ConcurrentHashMap<String, Int>()

        private val store: HttpServer =
            HttpServer.create(InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0).apply {
                createContext("/") { answer(it) }
                start()
            }

        private fun answer(exchange: HttpExchange) {
            val key = exchange.requestURI.path.substringAfter("/veds-files-test/")
            when {
                exchange.requestMethod == "DELETE" -> {
                    objects.remove(key)
                    exchange.sendResponseHeaders(NO_CONTENT, -1)
                }

                objects.containsKey(key) -> {
                    exchange.responseHeaders.set("Content-Length", objects.getValue(key).toString())
                    exchange.sendResponseHeaders(OK, -1)
                }

                else -> {
                    exchange.sendResponseHeaders(NOT_FOUND, -1)
                }
            }
            exchange.close()
        }

        @JvmStatic
        @DynamicPropertySource
        fun storeAddress(registry: DynamicPropertyRegistry) {
            registry.add("application.file.storage.endpoint") {
                "http://${store.address.hostString}:${store.address.port}"
            }
        }
    }
}
