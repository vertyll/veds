package com.vertyll.veds.shared.web.openapi

import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.security.SecurityRequirement
import io.swagger.v3.oas.models.security.SecurityScheme
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.env.Environment

/**
 * Gives every service's generated OpenAPI document a name and a bearer-token scheme.
 *
 * Left to its defaults springdoc emits `OpenAPI definition` / `v0` and no security scheme, so
 * every service would publish a document indistinguishable from the next and Swagger UI would
 * offer no Authorize button — which matters because almost every endpoint requires a JWT.
 *
 * Registered here rather than per service so the description stays identical everywhere; a
 * service that wants its own document simply declares its own [OpenAPI] bean.
 */
@Configuration
@ConditionalOnClass(OpenAPI::class)
@EnableConfigurationProperties(SharedOpenApiProperties::class)
internal class SharedOpenApiAutoConfiguration {
    /**
     * `spring.application.name` reaches Kotlin as a platform type, so the analyzer reads the
     * second elvis as dead. It is not: a service that sets no name gets [FALLBACK_NAME].
     */
    @Suppress("kotlin:S6619")
    @Bean
    @ConditionalOnMissingBean
    fun sharedOpenApi(
        properties: SharedOpenApiProperties,
        environment: Environment,
    ): OpenAPI {
        val name = properties.title ?: environment.getProperty("spring.application.name") ?: FALLBACK_NAME
        return OpenAPI()
            .info(
                Info()
                    .title(name)
                    .version(properties.version)
                    .description(
                        properties.description
                            ?: "REST API of $name. Endpoints require a Keycloak-issued bearer token unless stated otherwise.",
                    ),
            ).components(
                Components().addSecuritySchemes(
                    BEARER_SCHEME,
                    SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT"),
                ),
            ).addSecurityItem(SecurityRequirement().addList(BEARER_SCHEME))
    }

    private companion object {
        private const val BEARER_SCHEME = "bearer-jwt"
        private const val FALLBACK_NAME = "veds-service"
    }
}
