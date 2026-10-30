package com.vertyll.veds.shared.authz.client

import com.sun.net.httpserver.HttpServer
import com.vertyll.veds.sharedauthz.RoleScope
import com.vertyll.veds.sharedauthz.permissions
import org.awaitility.Awaitility.await
import org.springframework.boot.DefaultApplicationArguments
import org.springframework.web.client.HttpServerErrorException
import java.net.InetAddress
import java.net.InetSocketAddress
import java.time.Duration
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/** Runs against a real HTTP server standing in for iam-service, the one collaborator a test cannot run. */
internal class PermissionCatalogueRegistrationTest {
    private val received = CopyOnWriteArrayList<String>()
    private val refusalsLeft = AtomicInteger()
    private val iam =
        HttpServer.create(InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0).apply {
            createContext(CATALOGUE_PATH) { exchange ->
                received += exchange.requestBody.readAllBytes().decodeToString()
                val status = if (refusalsLeft.getAndDecrement() > 0) UNAVAILABLE else NO_CONTENT
                exchange.sendResponseHeaders(status, -1)
                exchange.close()
            }
            start()
        }
    private val properties =
        AuthzClientProperties(
            baseUrl = "http://${iam.address.hostString}:${iam.address.port}",
            registrationRetryInterval = Duration.ZERO,
        )
    private val registrar = PermissionCatalogueRegistrarAdapter(properties)
    private val catalogue =
        permissions("task") {
            permission("VIEW_TASKS", description = "See tasks")
            stockRole("TASK_VIEWER", RoleScope.PROJECT, "VIEW_TASKS")
        }

    @AfterTest
    fun stopTheServer() {
        iam.stop(0)
    }

    @Test
    fun `a catalogue reaches iam with its permissions and stock roles`() {
        registrar.register(catalogue)

        val body = received.single()
        assertContains(body, """"module":"task"""")
        assertContains(body, """"name":"VIEW_TASKS"""")
        assertContains(body, """"description":"See tasks"""")
        assertContains(body, """"name":"TASK_VIEWER"""")
    }

    @Test
    fun `a refused catalogue is the registrar's failure`() {
        refusalsLeft.set(1)

        assertFailsWith<HttpServerErrorException> { registrar.register(catalogue) }
    }

    @Test
    fun `registration keeps trying until iam answers`() {
        refusalsLeft.set(2)

        PermissionCatalogueRegistrationRunner(listOf(catalogue), registrar, properties)
            .run(DefaultApplicationArguments())

        await().atMost(TIMEOUT).until { received.size == 3 }
    }

    @Test
    fun `a service without a catalogue registers nothing`() {
        PermissionCatalogueRegistrationRunner(emptyList(), registrar, properties).run(DefaultApplicationArguments())

        assertEquals(emptyList(), received.toList())
    }

    @Test
    fun `the retry interval defaults to fifteen seconds`() {
        assertEquals(Duration.ofSeconds(15), AuthzClientProperties(baseUrl = "http://iam").registrationRetryInterval)
    }

    private companion object {
        const val CATALOGUE_PATH = "/internal/authz/catalogue"
        const val NO_CONTENT = 204
        const val UNAVAILABLE = 503
        val TIMEOUT: Duration = Duration.ofSeconds(10)
    }
}
