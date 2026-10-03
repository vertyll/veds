package com.vertyll.veds.shared.web.config

import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Configuration

/**
 * Autoconfiguration that registers [SharedKeycloakProperties] so the
 * `application.shared.*` namespace (Keycloak server URL, realm, admin client,
 * cookie settings, …) is available to every microservice via constructor
 * injection without each service having to declare
 * `@EnableConfigurationProperties` itself.
 *
 * Paired with [SharedConfigEnvironmentPostProcessor], which loads the
 * underlying `shared-config.yml` from the classpath.
 */
@Configuration
@EnableConfigurationProperties(SharedKeycloakProperties::class)
class SharedConfigAutoConfiguration
