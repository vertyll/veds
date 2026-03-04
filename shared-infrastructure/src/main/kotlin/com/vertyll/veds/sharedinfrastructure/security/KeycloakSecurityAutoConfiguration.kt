package com.vertyll.veds.sharedinfrastructure.security

import com.vertyll.veds.sharedinfrastructure.config.SharedConfigProperties
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.oauth2.jwt.Jwt

/**
 * Autoconfiguration that provides Keycloak JWT converters for both
 * WebMVC (servlet) and WebFlux (reactive) environments.
 */
@Configuration
@ConditionalOnClass(Jwt::class)
class KeycloakSecurityAutoConfiguration {
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
    fun keycloakJwtAuthenticationConverter(sharedConfig: SharedConfigProperties): KeycloakJwtAuthenticationConverter =
        KeycloakJwtAuthenticationConverter(sharedConfig)

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.REACTIVE)
    fun reactiveKeycloakJwtAuthenticationConverter(sharedConfig: SharedConfigProperties): ReactiveKeycloakJwtAuthenticationConverter =
        ReactiveKeycloakJwtAuthenticationConverter(sharedConfig)
}
