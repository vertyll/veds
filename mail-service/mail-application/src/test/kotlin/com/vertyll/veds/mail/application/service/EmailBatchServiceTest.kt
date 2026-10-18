package com.vertyll.veds.mail.application.service

import com.vertyll.veds.mail.application.port.inbound.EmailUseCase
import com.vertyll.veds.mail.domain.model.EmailTemplate
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class EmailBatchServiceTest {
    private val sent = mutableListOf<Pair<String, Map<String, String>>>()
    private var refuse = emptySet<String>()

    private val emails =
        object : EmailUseCase {
            override fun sendEmail(
                to: String,
                template: EmailTemplate,
                variables: Map<String, String>,
                replyTo: String?,
            ): Boolean {
                sent += to to variables
                return to !in refuse
            }

            override fun getEmailLogs() = error("not used by the batch")
        }

    private val service = EmailBatchService(emails)

    private fun send(
        recipients: List<String>,
        common: Map<String, String> = emptyMap(),
        specific: Map<String, Map<String, String>> = emptyMap(),
    ) = service.processEmailBatch(
        recipients = recipients,
        template = EmailTemplate.PROJECT_INVITATION,
        commonVariables = common,
        specificVariables = specific,
        replyTo = null,
    )

    @Test
    fun `every recipient gets one mail`() {
        val results = send(listOf(A_EXAMPLE_COM, B_EXAMPLE_COM))

        assertEquals(listOf(A_EXAMPLE_COM, B_EXAMPLE_COM), sent.map { it.first })
        assertEquals(mapOf(A_EXAMPLE_COM to true, B_EXAMPLE_COM to true), results)
    }

    @Test
    fun `common variables reach every recipient`() {
        send(listOf(A_EXAMPLE_COM, B_EXAMPLE_COM), common = mapOf(PROJECTNAME to APOLLO))

        assertTrue(sent.all { it.second[PROJECTNAME] == APOLLO })
    }

    @Test
    fun `a recipient's own variables override the common ones`() {
        send(
            listOf(A_EXAMPLE_COM, B_EXAMPLE_COM),
            common = mapOf(FIRSTNAME to "there", PROJECTNAME to APOLLO),
            specific = mapOf(A_EXAMPLE_COM to mapOf(FIRSTNAME to "Ada")),
        )

        assertEquals("Ada", sent.first { it.first == A_EXAMPLE_COM }.second[FIRSTNAME])
        assertEquals("there", sent.first { it.first == B_EXAMPLE_COM }.second[FIRSTNAME])
        assertEquals(APOLLO, sent.first { it.first == A_EXAMPLE_COM }.second[PROJECTNAME])
    }

    @Test
    fun `a refused recipient does not stop the rest`() {
        refuse = setOf(B_EXAMPLE_COM)

        val results = send(listOf(A_EXAMPLE_COM, B_EXAMPLE_COM, "c@example.com"))

        assertEquals(3, sent.size)
        assertEquals(mapOf(A_EXAMPLE_COM to true, B_EXAMPLE_COM to false, "c@example.com" to true), results)
    }

    @Test
    fun `an empty batch sends nothing`() {
        assertTrue(send(emptyList()).isEmpty())
        assertTrue(sent.isEmpty())
    }

    @Test
    fun `personalisation for an absent recipient is ignored`() {
        send(listOf(A_EXAMPLE_COM), specific = mapOf("nobody@example.com" to mapOf(FIRSTNAME to "Ghost")))

        assertEquals(1, sent.size)
        assertTrue(sent.single().second.isEmpty())
    }
}

private const val A_EXAMPLE_COM = "a@example.com"
private const val B_EXAMPLE_COM = "b@example.com"
private const val FIRSTNAME = "firstName"
private const val PROJECTNAME = "projectName"
private const val APOLLO = "Apollo"
