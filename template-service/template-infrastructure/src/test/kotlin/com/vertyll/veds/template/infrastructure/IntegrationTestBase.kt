package com.vertyll.veds.template.infrastructure

import com.vertyll.veds.shared.translation.client.TranslationClientProperties
import org.junit.jupiter.api.Tag
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Import
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
@Import(IntegrationTestBase.TranslationClient::class)
abstract class IntegrationTestBase protected constructor() {
    // TemplateServiceApplication does not enable these properties, as every other service does, so the service
    // itself cannot start. The context here enables them to reach what lies behind that.
    @TestConfiguration
    @EnableConfigurationProperties(TranslationClientProperties::class)
    class TranslationClient

    companion object {
        private val postgres: PostgreSQLContainer =
            PostgreSQLContainer(
                DockerImageName.parse("postgres:18-alpine"),
            ).withStartupTimeout(Duration.ofMinutes(2))
                .withStartupAttempts(3)
                .withDatabaseName("template_service_test")
                .withUsername("postgres")
                .withPassword("postgres")
                .apply { start() }

        private val kafka: KafkaContainer =
            KafkaContainer(
                DockerImageName.parse("apache/kafka-native:4.4.0"),
            ).withStartupTimeout(Duration.ofMinutes(2))
                .withStartupAttempts(3)
                .apply { start() }

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
