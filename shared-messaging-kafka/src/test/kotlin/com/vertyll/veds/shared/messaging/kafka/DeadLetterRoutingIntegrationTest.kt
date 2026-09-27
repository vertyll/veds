package com.vertyll.veds.shared.messaging.kafka

import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.ByteArrayDeserializer
import org.apache.kafka.common.serialization.ByteArraySerializer
import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.kafka.common.serialization.StringSerializer
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.springframework.kafka.core.DefaultKafkaProducerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.listener.CommonErrorHandler
import org.springframework.kafka.listener.ContainerProperties
import org.springframework.kafka.listener.KafkaMessageListenerContainer
import org.springframework.kafka.listener.MessageListener
import org.testcontainers.kafka.KafkaContainer
import org.testcontainers.utility.DockerImageName
import java.time.Duration
import java.util.Properties
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Proves the dead letter route end to end: a listener that always throws must exhaust its retries
 * and have the record published to `<topic>-dlt`.
 *
 * The topic name is the point. Spring's recoverer defaults to `<topic>.DLT`, while
 * `infra/kafka/topics.tf` provisions `<topic>-dlt`; with `auto.create.topics.enable=false` the
 * default name resolves to no topic, so an exhausted message is never recovered and the
 * provisioned dead letter topics stay empty. Only a real broker shows that.
 */
@Tag("integration")
class DeadLetterRoutingIntegrationTest {
    private companion object {
        const val TOPIC = "dlt-routing-test"
        const val DLT = "$TOPIC-dlt"
        const val MAX_RETRIES = 3L
        const val EXPECTED_ATTEMPTS = MAX_RETRIES + 1
    }

    private val kafka: KafkaContainer =
        KafkaContainer(DockerImageName.parse("apache/kafka-native:4.3.1")).also { it.start() }

    private fun producerFactory() =
        DefaultKafkaProducerFactory<String, ByteArray>(
            mapOf(
                ProducerConfig.BOOTSTRAP_SERVERS_CONFIG to kafka.bootstrapServers,
                ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG to StringSerializer::class.java,
                ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG to ByteArraySerializer::class.java,
            ),
        )

    @Test
    fun `a handler that keeps failing sends the record to the dash-dlt topic`() {
        val template = KafkaTemplate(producerFactory())
        val errorHandler: CommonErrorHandler = KafkaTemplateAutoConfiguration().kafkaErrorHandler(template)
        val attempts = AtomicInteger()

        KafkaProducer<String, ByteArray>(
            Properties().apply {
                put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.bootstrapServers)
                put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer::class.java.name)
                put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, ByteArraySerializer::class.java.name)
            },
        ).use { it.send(ProducerRecord(TOPIC, "k", "payload".toByteArray())).get() }

        val container = failingListenerContainer(errorHandler, attempts)
        container.start()
        try {
            val dead = pollDeadLetterTopic()
            assertEquals(1, dead.size, "the exhausted record must arrive on $DLT")
            assertEquals("payload", String(dead.first()))
            assertTrue(
                attempts.get() >= EXPECTED_ATTEMPTS,
                "the handler must actually be retried before recovery, was ${attempts.get()}",
            )
        } finally {
            container.stop()
        }
    }

    private fun failingListenerContainer(
        errorHandler: CommonErrorHandler,
        attempts: AtomicInteger,
    ): KafkaMessageListenerContainer<String, ByteArray> {
        val consumerFactory =
            org.springframework.kafka.core.DefaultKafkaConsumerFactory<String, ByteArray>(
                mapOf(
                    ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG to kafka.bootstrapServers,
                    ConsumerConfig.GROUP_ID_CONFIG to "dlt-routing-test-group",
                    ConsumerConfig.AUTO_OFFSET_RESET_CONFIG to "earliest",
                    ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG to StringDeserializer::class.java,
                    ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG to ByteArrayDeserializer::class.java,
                ),
            )
        val properties =
            ContainerProperties(TOPIC).apply {
                setMessageListener(
                    MessageListener<String, ByteArray> {
                        attempts.incrementAndGet()
                        error("handler always fails")
                    },
                )
            }
        return KafkaMessageListenerContainer(consumerFactory, properties).apply {
            setCommonErrorHandler(errorHandler)
        }
    }

    private fun pollDeadLetterTopic(): List<ByteArray> {
        KafkaConsumer<String, ByteArray>(
            Properties().apply {
                put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.bootstrapServers)
                put(ConsumerConfig.GROUP_ID_CONFIG, "dlt-routing-test-verifier")
                put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest")
                put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer::class.java.name)
                put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ByteArrayDeserializer::class.java.name)
            },
        ).use { consumer ->
            consumer.subscribe(listOf(DLT))
            val deadline = System.currentTimeMillis() + Duration.ofSeconds(60).toMillis()
            while (System.currentTimeMillis() < deadline) {
                val records = consumer.poll(Duration.ofMillis(500))
                if (!records.isEmpty) return records.map { it.value() }
            }
            return emptyList()
        }
    }
}
