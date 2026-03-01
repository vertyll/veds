package com.vertyll.veds.sharedinfrastructure.kafka

import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.kafka.common.serialization.StringSerializer
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory
import org.springframework.kafka.core.ConsumerFactory
import org.springframework.kafka.core.DefaultKafkaConsumerFactory
import org.springframework.kafka.core.DefaultKafkaProducerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.core.ProducerFactory
import org.springframework.kafka.listener.CommonErrorHandler
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer
import org.springframework.kafka.listener.DefaultErrorHandler
import org.springframework.util.backoff.FixedBackOff

/**
 * Autoconfiguration for Kafka Producer and Consumer.
 * Only creates beans when kafka is enabled.
 */
@Configuration
@ConditionalOnProperty(
    name = ["spring.kafka.enabled"],
    havingValue = "true",
    matchIfMissing = true,
)
class KafkaTemplateAutoConfiguration {
    private val logger = LoggerFactory.getLogger(javaClass)

    private companion object {
        /** Interval between retry attempts in milliseconds. */
        private const val RETRY_INTERVAL_MS = 1000L

        /** Maximum number of retry attempts before sending to DLT. */
        private const val MAX_RETRIES = 3L
    }

    @Bean
    fun producerFactory(
        @Value($$"${spring.kafka.bootstrap-servers:localhost:29092}")
        bootstrapServers: String,
    ): ProducerFactory<String, String> {
        val configProps =
            mapOf<String, Any>(
                ProducerConfig.BOOTSTRAP_SERVERS_CONFIG to bootstrapServers,
                ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG to StringSerializer::class.java,
                ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG to StringSerializer::class.java,
            )
        return DefaultKafkaProducerFactory(configProps)
    }

    @Bean
    fun kafkaTemplate(producerFactory: ProducerFactory<String, String>) = KafkaTemplate(producerFactory)

    @Bean
    fun consumerFactory(
        @Value($$"${spring.kafka.bootstrap-servers:localhost:29092}")
        bootstrapServers: String,
        @Value($$"${spring.kafka.consumer.group-id:default-group}")
        groupId: String,
    ): ConsumerFactory<String, String> {
        val configProps =
            mapOf<String, Any>(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG to bootstrapServers,
                ConsumerConfig.GROUP_ID_CONFIG to groupId,
                ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG to StringDeserializer::class.java,
                ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG to StringDeserializer::class.java,
                ConsumerConfig.AUTO_OFFSET_RESET_CONFIG to "earliest",
            )
        return DefaultKafkaConsumerFactory(configProps)
    }

    /**
     * Error handler that retries failed messages [MAX_RETRIES] times with a
     * [RETRY_INTERVAL_MS] interval, then publishes to a Dead Letter Topic
     * (original-topic.DLT) via [DeadLetterPublishingRecoverer].
     */
    @Bean
    fun kafkaErrorHandler(kafkaTemplate: KafkaTemplate<String, String>): CommonErrorHandler {
        val recoverer = DeadLetterPublishingRecoverer(kafkaTemplate)
        val backOff = FixedBackOff(RETRY_INTERVAL_MS, MAX_RETRIES)
        val errorHandler = DefaultErrorHandler(recoverer, backOff)
        logger.info(
            "Kafka error handler configured: {} retries with {}ms interval, then DLT",
            MAX_RETRIES,
            RETRY_INTERVAL_MS,
        )
        return errorHandler
    }

    @Bean
    fun kafkaListenerContainerFactory(
        consumerFactory: ConsumerFactory<String, String>,
        kafkaErrorHandler: CommonErrorHandler,
    ): ConcurrentKafkaListenerContainerFactory<String, String> {
        val factory = ConcurrentKafkaListenerContainerFactory<String, String>()
        factory.setConsumerFactory(consumerFactory)
        factory.setCommonErrorHandler(kafkaErrorHandler)
        return factory
    }
}
