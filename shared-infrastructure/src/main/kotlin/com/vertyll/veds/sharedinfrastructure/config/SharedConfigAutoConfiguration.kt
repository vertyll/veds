package com.vertyll.veds.sharedinfrastructure.config

import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Configuration

/**
 * Autoconfiguration that registers [SharedConfigProperties] so the
 * `veds.shared.*` namespace (Keycloak server URL, realm, admin client,
 * cookie settings, …) is available to every microservice via constructor
 * injection without each service having to declare
 * `@EnableConfigurationProperties` itself.
 *
 * Paired with [SharedConfigEnvironmentPostProcessor], which loads the
 * underlying `shared-config.yml` from the classpath.
 */
@Configuration
@EnableConfigurationProperties(SharedConfigProperties::class)
class SharedConfigAutoConfiguration
