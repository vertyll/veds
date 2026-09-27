package com.vertyll.veds.shared.web.config

import org.springframework.boot.EnvironmentPostProcessor
import org.springframework.boot.SpringApplication
import org.springframework.boot.env.YamlPropertySourceLoader
import org.springframework.core.env.ConfigurableEnvironment
import org.springframework.core.env.Profiles
import org.springframework.core.io.ClassPathResource

/**
 * Loads `shared-web-config.yml` from the classpath, and `shared-web-config-local.yml` on top of it when the `local` or
 * `test` profile is in effect, so no microservice has to import them in its own `application.yml`.
 *
 * Added **last**, so these are defaults: a service's own `application-*.yml`, a profile or an
 * environment variable all override them. The base file carries no local fallbacks: in every other
 * profile each endpoint and credential has to come from the environment.
 */
internal class SharedConfigEnvironmentPostProcessor : EnvironmentPostProcessor {
    private val loader = YamlPropertySourceLoader()

    override fun postProcessEnvironment(
        environment: ConfigurableEnvironment,
        application: SpringApplication,
    ) {
        if (environment.acceptsProfiles(Profiles.of(DEVELOPMENT_PROFILES))) {
            load(environment, "shared-web-config-local")
        }
        load(environment, "shared-web-config")
    }

    private fun load(
        environment: ConfigurableEnvironment,
        name: String,
    ) {
        loader.load(name, ClassPathResource("$name.yml")).forEach { environment.propertySources.addLast(it) }
    }

    private companion object {
        const val DEVELOPMENT_PROFILES = "local | test"
    }
}
