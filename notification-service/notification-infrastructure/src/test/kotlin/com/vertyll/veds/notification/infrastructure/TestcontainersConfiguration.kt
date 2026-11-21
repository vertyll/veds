package com.vertyll.veds.notification.infrastructure

import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.context.annotation.Bean
import org.springframework.test.context.DynamicPropertyRegistrar
import org.testcontainers.kafka.KafkaContainer
import org.testcontainers.postgresql.PostgreSQLContainer
import org.testcontainers.utility.DockerImageName
import java.time.Duration

@TestConfiguration(proxyBeanMethods = false)
internal class TestcontainersConfiguration {
    @Bean
    @ServiceConnection
    fun postgresContainer(): PostgreSQLContainer =
        PostgreSQLContainer(
            DockerImageName.parse("postgres:18-alpine"),
        ).withStartupTimeout(Duration.ofMinutes(2))
            .withStartupAttempts(3)
            .withDatabaseName("notification_service_test")
            .withUsername("postgres")
            .withPassword("postgres")

    @Bean
    fun kafkaContainer(): KafkaContainer =
        KafkaContainer(
            DockerImageName.parse("apache/kafka-native:4.4.0"),
        ).withStartupTimeout(Duration.ofMinutes(2))
            .withStartupAttempts(3)

    // The messaging library binds its own spring.kafka.* properties rather than Spring Boot's Kafka
    // autoconfiguration, so no @ServiceConnection reaches it and the broker's address is given as a property
    @Bean
    fun kafkaProperties(kafka: KafkaContainer): DynamicPropertyRegistrar =
        DynamicPropertyRegistrar { registry ->
            registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers)
        }
}
