package com.vertyll.veds.iam.infrastructure.config

import com.vertyll.veds.sharedinfrastructure.security.KeycloakJwtAuthenticationConverter
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.web.SecurityFilterChain

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
class SecurityConfig(
    private val keycloakJwtAuthenticationConverter: KeycloakJwtAuthenticationConverter,
) {
    companion object {
        private val PUBLIC_AUTH_ENDPOINTS =
            arrayOf(
                "/auth/register",
                "/auth/activate",
                "/auth/reset-password-request",
                "/auth/confirm-reset-password",
                "/auth/resend-activation",
                "/auth/confirm-email-change",
                "/auth/confirm-password-change",
                "/auth/set-new-password",
            )
    }

    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .csrf { it.disable() }
            .cors { it.disable() }
            .formLogin { it.disable() }
            .httpBasic { it.disable() }
            .authorizeHttpRequests { auth ->
                auth
                    .requestMatchers("/actuator/**")
                    .permitAll()
                    .requestMatchers(*PUBLIC_AUTH_ENDPOINTS)
                    .permitAll()
                    .anyRequest()
                    .authenticated()
            }.sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .oauth2ResourceServer { oauth2 ->
                oauth2.jwt { jwt ->
                    jwt.jwtAuthenticationConverter(keycloakJwtAuthenticationConverter)
                }
            }

        return http.build()
    }
}
