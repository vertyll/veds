package com.vertyll.veds.sharedinfrastructure.kafka

import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Configuration

/**
 * Autoconfiguration class for Kafka topics.
 * Enables the KafkaTopicsConfig properties and makes them available for injection.
 */
@Configuration
@EnableConfigurationProperties(KafkaTopicsConfig::class)
class KafkaConfigAutoConfiguration
