package com.vertyll.veds.shared.translation.client

import com.sun.net.httpserver.HttpServer
import com.vertyll.veds.sharedtranslation.translations
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

/** Runs against a real HTTP server standing in for translation-service, which a test cannot run. */
internal class TranslationCatalogueRegistrationTest {
    private val received = CopyOnWriteArrayList<String>()
    private val refusalsLeft = AtomicInteger()
    private val translation =
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
        TranslationClientProperties(
            baseUrl = "http://${translation.address.hostString}:${translation.address.port}",
            registrationRetryInterval = Duration.ZERO,
        )
    private val registrar = TranslationCatalogueRegistrarAdapter(properties)
    private val catalogue =
        translations("task-service") {
            key("task.not_found") {
                pl("Nie znaleziono zadania.")
                en("Task not found.")
            }
        }

    @AfterTest
    fun stopTheServer() {
        translation.stop(0)
    }

    @Test
    fun `a catalogue reaches the service with its keys and default values`() {
        registrar.register(catalogue)

        val body = received.single()
        assertContains(body, """"sourceService":"task-service"""")
        assertContains(body, """"key":"task.not_found"""")
        assertContains(body, """"en":"Task not found."""")
    }

    @Test
    fun `a refused catalogue is the registrar's failure`() {
        refusalsLeft.set(1)

        assertFailsWith<HttpServerErrorException> { registrar.register(catalogue) }
    }

    @Test
    fun `registration keeps trying until the service answers`() {
        refusalsLeft.set(2)

        TranslationCatalogueRegistrationRunner(listOf(catalogue), registrar, properties)
            .run(DefaultApplicationArguments())

        await().atMost(TIMEOUT).until { received.size == 3 }
    }

    @Test
    fun `a service without a catalogue registers nothing`() {
        TranslationCatalogueRegistrationRunner(emptyList(), registrar, properties).run(DefaultApplicationArguments())

        assertEquals(emptyList(), received.toList())
    }

    @Test
    fun `the retry interval defaults to fifteen seconds`() {
        assertEquals(Duration.ofSeconds(15), TranslationClientProperties(baseUrl = "http://t").registrationRetryInterval)
    }

    private companion object {
        const val CATALOGUE_PATH = "/internal/translations/catalogue"
        const val NO_CONTENT = 204
        const val UNAVAILABLE = 503
        val TIMEOUT: Duration = Duration.ofSeconds(10)
    }
}
