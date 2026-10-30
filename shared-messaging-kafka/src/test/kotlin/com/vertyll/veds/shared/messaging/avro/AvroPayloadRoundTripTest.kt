package com.vertyll.veds.shared.messaging.avro

import com.vertyll.veds.shared.messaging.kafka.KafkaInfraProperties
import org.apache.avro.util.ClassSecurityValidator
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class AvroPayloadRoundTripTest {
    private val properties =
        KafkaInfraProperties(
            bootstrapServers = "broker:9092",
            properties = mapOf("schema.registry.url" to "mock://avro-round-trip"),
            security = KafkaInfraProperties.Security("PLAINTEXT"),
            ssl = KafkaInfraProperties.Ssl("", "PKCS12", ""),
            consumer = KafkaInfraProperties.Consumer("group", "earliest"),
        )
    private val configuration = AvroSerDeAutoConfiguration()

    @Test
    fun `what is serialized for a topic reads back the same`() {
        val payload = configuration.avroPayloadSerializer(properties).serialize("orders", "placed")

        val read = configuration.avroPayloadDeserializer(properties).deserialize("orders", payload)

        assertEquals("placed", read.toString())
    }

    @Test
    fun `the project's own event classes are trusted and strangers are not`() {
        ClassSecurityValidator.validate(AvroPayloadRoundTripTest::class.java)

        assertFailsWith<SecurityException> { ClassSecurityValidator.validate(Runtime::class.java) }
    }
}
