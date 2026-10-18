package com.vertyll.veds.shared.web.security

import com.vertyll.veds.shared.web.config.SharedKeycloakProperties
import com.vertyll.veds.sharedauthz.RolePermissionsSource
import org.springframework.beans.factory.ObjectProvider
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.oauth2.jwt.Jwt
import tools.jackson.databind.ObjectMapper

@Configuration
@ConditionalOnClass(Jwt::class)
internal class KeycloakSecurityAutoConfiguration {
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
    fun keycloakJwtAuthenticationConverter(sharedConfig: SharedKeycloakProperties): KeycloakJwtAuthenticationConverter =
        KeycloakJwtAuthenticationConverter(sharedConfig)

    /**
     * Answers `@PreAuthorize("@authz.has('…')")`. Registered for every service, so
     * a guard written against a permission behaves the same everywhere; one
     * without a [RolePermissionsSource] grants nothing rather than falling back
     * to the role in the token.
     */
    @Bean("authz")
    @ConditionalOnMissingBean(name = ["authz"])
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
    fun permissionAuthorizer(source: ObjectProvider<RolePermissionsSource>): PermissionAuthorizer =
        PermissionAuthorizer(source.getIfAvailable())

    /**
     * Makes an unauthenticated request answer in the same shape as every other
     * refusal. A service opts in by naming it in its `oauth2ResourceServer`
     * configuration.
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
    fun problemAuthenticationEntryPoint(objectMapper: ObjectMapper): ProblemAuthenticationEntryPoint =
        ProblemAuthenticationEntryPoint(objectMapper)

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.REACTIVE)
    fun reactiveKeycloakJwtAuthenticationConverter(sharedConfig: SharedKeycloakProperties): ReactiveKeycloakJwtAuthenticationConverter =
        ReactiveKeycloakJwtAuthenticationConverter(sharedConfig)
}
