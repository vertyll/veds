package com.vertyll.veds.mail.infrastructure.kafka

import com.vertyll.veds.iam.RolePermissionsChangedEvent
import com.vertyll.veds.mail.infrastructure.IntegrationTestBase
import com.vertyll.veds.mail.infrastructure.persistence.repository.RolePermissionsJpaRepository
import com.vertyll.veds.shared.messaging.avro.AvroPayloadSerializer
import com.vertyll.veds.shared.messaging.event.Events
import com.vertyll.veds.sharedauthz.PermissionCatalogue
import com.vertyll.veds.sharedauthz.RolePermissionsSource
import org.apache.kafka.clients.producer.ProducerRecord
import org.awaitility.Awaitility.await
import org.junit.jupiter.api.Test
import org.springframework.data.repository.findByIdOrNull
import org.springframework.kafka.core.KafkaTemplate
import java.nio.charset.StandardCharsets
import java.time.Duration
import kotlin.test.assertEquals
import kotlin.test.assertFalse

internal class RolePermissionsIntegrationTest(
    private val kafka: KafkaTemplate<String, ByteArray>,
    private val serializer: AvroPayloadSerializer,
    private val roles: RolePermissionsJpaRepository,
    private val source: RolePermissionsSource,
    private val catalogue: PermissionCatalogue,
) : IntegrationTestBase() {
    private fun change(
        role: String,
        scope: String = "GLOBAL",
        removed: Boolean = false,
        permissions: List<String> = emptyList(),
    ) {
        val eventId = Events.newId()
        val event =
            RolePermissionsChangedEvent
                .newBuilder()
                .setEventId(eventId)
                .setTimestamp(Events.now())
                .setRole(role)
                .setScope(scope)
                .setRemoved(removed)
                .setUnrestricted(false)
                .setPermissions(permissions)
                .build()
        val record = ProducerRecord<String, ByteArray>(TOPIC, eventId, serializer.serialize(TOPIC, event))
        record.headers().add("eventId", eventId.toByteArray(StandardCharsets.UTF_8))
        kafka.send(record).get()
    }

    @Test
    fun `a role keeps only the permissions this service knows`() {
        val known = catalogue.names.first()

        change("EDITOR", permissions = listOf(known, "SOMEONE_ELSES_PERMISSION"))

        await().atMost(TIMEOUT).until { roles.existsById("EDITOR") }
        assertEquals(setOf(known), source.forRoles(listOf("EDITOR")))
        assertFalse(source.isUnrestricted(listOf("EDITOR")))
    }

    @Test
    fun `a removed role grants nothing any more`() {
        change("TEMPORARY", permissions = listOf(catalogue.names.first()))
        await().atMost(TIMEOUT).until { roles.existsById("TEMPORARY") }

        change("TEMPORARY", removed = true)

        await().atMost(TIMEOUT).until { roles.findByIdOrNull("TEMPORARY") == null }
        assertEquals(emptySet(), source.forRoles(listOf("TEMPORARY")))
    }

    @Test
    fun `a project role is ignored, as mail belongs to no project`() {
        change("PROJECT_VIEWER", scope = "PROJECT", permissions = listOf(catalogue.names.first()))
        change("AFTER_THE_PROJECT_ROLE", permissions = listOf(catalogue.names.first()))

        await().atMost(TIMEOUT).until { roles.existsById("AFTER_THE_PROJECT_ROLE") }
        assertFalse(roles.existsById("PROJECT_VIEWER"))
    }

    private companion object {
        const val TOPIC = "role-permissions-changed"
        val TIMEOUT: Duration = Duration.ofSeconds(30)
    }
}
