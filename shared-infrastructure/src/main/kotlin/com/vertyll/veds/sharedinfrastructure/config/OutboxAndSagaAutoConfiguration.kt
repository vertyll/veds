package com.vertyll.veds.sharedinfrastructure.config
import com.vertyll.veds.sharedinfrastructure.kafka.KafkaOutboxProperties
import com.vertyll.veds.sharedinfrastructure.saga.SagaProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.EnableScheduling

/**
 * Autoconfiguration registering infrastructure-wide @ConfigurationProperties
 * and enabling scheduling for the Kafka outbox processor and saga watchdog.
 */
@Configuration
@EnableScheduling
@EnableConfigurationProperties(
    KafkaOutboxProperties::class,
    SagaProperties::class,
)
internal class OutboxAndSagaAutoConfiguration
