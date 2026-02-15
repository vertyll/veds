package com.vertyll.projecta.gateway.config

import com.vertyll.projecta.gateway.security.JwtAuthFilter
import com.vertyll.projecta.sharedinfrastructure.role.RoleType
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpStatus
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity
import org.springframework.security.config.web.server.SecurityWebFiltersOrder
import org.springframework.security.config.web.server.ServerHttpSecurity
import org.springframework.security.web.server.SecurityWebFilterChain
import org.springframework.security.web.server.authentication.HttpStatusServerEntryPoint
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.reactive.CorsConfigurationSource
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource

@Configuration
@EnableWebFluxSecurity
class SecurityConfig(
    private val jwtAuthFilter: JwtAuthFilter,
) {
    companion object {
        // Public Auth endpoints
        private val PUBLIC_AUTH_ENDPOINTS =
            arrayOf(
                "/auth/register",
                "/auth/authenticate",
                "/auth/refresh-token",
                "/auth/activate",
                "/auth/reset-password-request",
                "/auth/confirm-reset-password",
                "/auth/resend-activation",
            )

        // Swagger documentation endpoints
        private val SWAGGER_ENDPOINTS =
            arrayOf(
                "/swagger-ui.html",
                "/api-docs/**",
                "/v3/api-docs/**",
                "/swagger-ui/**",
            )

        // Actuator endpoints
        private const val ACTUATOR_ENDPOINTS = "/actuator/**"

        // Protected Auth endpoints
        private val PROTECTED_AUTH_ENDPOINTS =
            arrayOf(
                "/auth/me",
                "/auth/logout",
                "/auth/change-password-request",
                "/auth/change-email-request",
                "/auth/confirm-email-change",
                "/auth/confirm-password-change",
                "/auth/set-new-password",
                "/auth/sessions/**",
            )

        // Role endpoints
        private const val ROLE_ADMIN_ENDPOINTS = "/roles/admin/**"
        private const val ROLE_USER_ENDPOINTS = "/roles/**"

        // User endpoints
        private const val USER_ADMIN_ENDPOINTS = "/users/admin/**"
        private const val USER_PROFILE_ENDPOINT = "/users/me"
        private const val USER_ID_ENDPOINT = "/users/{id}"

        // Mail endpoints
        private const val MAIL_ENDPOINTS = "/mail/**"
    }

    @Bean
    fun springSecurityFilterChain(http: ServerHttpSecurity): SecurityWebFilterChain =
        http
            .csrf { it.disable() } // Disable CSRF for API Gateway
            .formLogin { it.disable() } // Disable default login form
            .httpBasic { it.disable() } // Disable HTTP Basic Auth
            .exceptionHandling {
                it.authenticationEntryPoint(HttpStatusServerEntryPoint(HttpStatus.UNAUTHORIZED))
            }.authorizeExchange { exchanges ->
                exchanges
                    // Public endpoints that don't require authentication
                    .pathMatchers(*PUBLIC_AUTH_ENDPOINTS)
                    .permitAll()
                    // Swagger docs
                    .pathMatchers(*SWAGGER_ENDPOINTS)
                    .permitAll()
                    // Health and metrics endpoints
                    .pathMatchers(ACTUATOR_ENDPOINTS)
                    .permitAll()
                    // Auth service - some endpoints need authentication
                    .pathMatchers(*PROTECTED_AUTH_ENDPOINTS)
                    .authenticated()
                    // Role service admin endpoints
                    .pathMatchers(ROLE_ADMIN_ENDPOINTS)
                    .hasRole(RoleType.ADMIN.value)
                    // Role service regular endpoints
                    .pathMatchers(ROLE_USER_ENDPOINTS)
                    .authenticated()
                    // User service admin endpoints
                    .pathMatchers(USER_ADMIN_ENDPOINTS)
                    .hasRole(RoleType.ADMIN.value)
                    // User service user endpoints
                    .pathMatchers(USER_PROFILE_ENDPOINT)
                    .authenticated()
                    // For user ID paths, we need a simpler approach in WebFlux
                    .pathMatchers(USER_ID_ENDPOINT)
                    .authenticated()
                    // Mail service endpoints (admin only)
                    .pathMatchers(MAIL_ENDPOINTS)
                    .hasRole(RoleType.ADMIN.value)
                    // Default policy - deny all
                    .anyExchange()
                    .authenticated()
            }
            // Add JWT filter before UsernamePasswordAuthenticationFilter
            .addFilterAt(jwtAuthFilter, SecurityWebFiltersOrder.AUTHENTICATION)
            .build()

    @Bean
    fun corsConfigurationSource(): CorsConfigurationSource {
        val configuration = CorsConfiguration()
        configuration.allowedOrigins = listOf("*")
        configuration.allowedMethods = listOf("GET", "POST", "PUT", "DELETE", "OPTIONS")
        configuration.allowedHeaders = listOf("*")

        val source = UrlBasedCorsConfigurationSource()
        source.registerCorsConfiguration("/**", configuration)
        return source
    }
}
