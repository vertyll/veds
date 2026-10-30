package com.vertyll.veds.shared.messaging.kafka

import org.apache.kafka.clients.CommonClientConfigs
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.config.SslConfigs
import org.springframework.kafka.core.DefaultKafkaConsumerFactory
import org.springframework.kafka.core.DefaultKafkaProducerFactory
import org.springframework.kafka.core.KafkaTemplate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertSame

class KafkaTemplateAutoConfigurationTest {
    private val configuration = KafkaTemplateAutoConfiguration()

    private fun properties(
        protocol: String = "PLAINTEXT",
        trustStore: String = "",
    ) = KafkaInfraProperties(
        bootstrapServers = "broker:9092",
        properties = mapOf("schema.registry.url" to "mock://registry"),
        security = KafkaInfraProperties.Security(protocol),
        ssl = KafkaInfraProperties.Ssl(trustStore, "PKCS12", "secret"),
        consumer = KafkaInfraProperties.Consumer("group", "earliest"),
    )

    private fun producerConfig(properties: KafkaInfraProperties) =
        (configuration.producerFactory(properties) as DefaultKafkaProducerFactory).configurationProperties

    private fun consumerConfig(properties: KafkaInfraProperties) =
        (configuration.consumerFactory(properties) as DefaultKafkaConsumerFactory).configurationProperties

    @Test
    fun `the schema registry is read from the kafka properties`() {
        assertEquals("mock://registry", properties().schemaRegistryUrl)
    }

    @Test
    fun `plaintext clients carry no security settings`() {
        val config = producerConfig(properties())

        assertEquals("broker:9092", config[ProducerConfig.BOOTSTRAP_SERVERS_CONFIG])
        assertFalse(config.containsKey(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG))
    }

    @Test
    fun `tls clients trust the configured store`() {
        val config = consumerConfig(properties(protocol = "SSL", trustStore = "/certs/ca.p12"))

        assertEquals("SSL", config[CommonClientConfigs.SECURITY_PROTOCOL_CONFIG])
        assertEquals("/certs/ca.p12", config[SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG])
        assertEquals("PKCS12", config[SslConfigs.SSL_TRUSTSTORE_TYPE_CONFIG])
    }

    @Test
    fun `tls without a store falls back to the system's trust`() {
        val config = producerConfig(properties(protocol = "SSL"))

        assertEquals("SSL", config[CommonClientConfigs.SECURITY_PROTOCOL_CONFIG])
        assertFalse(config.containsKey(SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG))
    }

    @Test
    fun `consumers join their group and start where configured`() {
        val config = consumerConfig(properties())

        assertEquals("group", config[ConsumerConfig.GROUP_ID_CONFIG])
        assertEquals("earliest", config[ConsumerConfig.AUTO_OFFSET_RESET_CONFIG])
    }

    @Test
    fun `listeners use the consumers and the dead letter handling built here`() {
        val producers = configuration.producerFactory(properties())
        val consumers = configuration.consumerFactory(properties())
        val errorHandler = configuration.kafkaErrorHandler(KafkaTemplate(producers))

        val factory = configuration.kafkaListenerContainerFactory(consumers, errorHandler)

        assertSame<Any?>(consumers, factory.consumerFactory)
        assertNotNull(configuration.kafkaTemplate(producers))
    }
}
