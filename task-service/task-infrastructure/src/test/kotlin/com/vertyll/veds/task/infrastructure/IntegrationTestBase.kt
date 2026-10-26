package com.vertyll.veds.task.infrastructure

import org.junit.jupiter.api.Tag
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.kafka.KafkaContainer
import org.testcontainers.postgresql.PostgreSQLContainer
import org.testcontainers.utility.DockerImageName
import java.time.Duration

@Tag("integration")
@SpringBootTest
@ActiveProfiles("test")
abstract class IntegrationTestBase protected constructor() {
    companion object {
        private val postgres: PostgreSQLContainer =
            PostgreSQLContainer(
                DockerImageName.parse("postgres:18-alpine"),
            ).withStartupTimeout(Duration.ofMinutes(2))
                .withStartupAttempts(3)
                .withDatabaseName("task_service_test")
                .withUsername("postgres")
                .withPassword("postgres")
                .also { it.start() }

        private val kafka: KafkaContainer =
            KafkaContainer(
                DockerImageName.parse("apache/kafka-native:4.4.0"),
            ).withStartupTimeout(Duration.ofMinutes(2))
                .withStartupAttempts(3)
                .also { it.start() }

        @JvmStatic
        @DynamicPropertySource
        fun properties(registry: DynamicPropertyRegistry) {
            registry.add("spring.datasource.url", postgres::getJdbcUrl)
            registry.add("spring.datasource.username", postgres::getUsername)
            registry.add("spring.datasource.password", postgres::getPassword)
            registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers)
        }
    }
}
