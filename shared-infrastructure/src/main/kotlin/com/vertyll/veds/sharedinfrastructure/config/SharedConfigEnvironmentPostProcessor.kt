package com.vertyll.veds.sharedinfrastructure.config

import org.springframework.boot.EnvironmentPostProcessor
import org.springframework.boot.SpringApplication
import org.springframework.boot.env.YamlPropertySourceLoader
import org.springframework.core.env.ConfigurableEnvironment
import org.springframework.core.io.ClassPathResource

/**
 * EnvironmentPostProcessor that automatically loads shared-config.yml from the classpath.
 * This eliminates the need for each microservice to manually import it in application.yml.
 */
class SharedConfigEnvironmentPostProcessor : EnvironmentPostProcessor {
    private val loader = YamlPropertySourceLoader()

    override fun postProcessEnvironment(
        environment: ConfigurableEnvironment,
        application: SpringApplication,
    ) {
        val resource = ClassPathResource("shared-config.yml")
        if (resource.exists()) {
            val propertySources = loader.load("shared-config", resource)
            propertySources.forEach {
                environment.propertySources.addFirst(it)
            }
        }
    }
}
