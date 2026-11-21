package com.vertyll.veds.apigateway

import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.context.annotation.Bean
import org.testcontainers.containers.GenericContainer
import java.time.Duration

@TestConfiguration(proxyBeanMethods = false)
internal class TestcontainersConfiguration {
    @Bean
    @ServiceConnection(name = "redis")
    fun redisContainer(): GenericContainer<*> =
        GenericContainer(
            "redis:8-alpine",
        ).withStartupTimeout(Duration.ofMinutes(2)).withStartupAttempts(3).withExposedPorts(REDIS_PORT)

    private companion object {
        private const val REDIS_PORT = 6379
    }
}
