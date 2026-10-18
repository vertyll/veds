package com.vertyll.veds.translation.infrastructure.config

import com.vertyll.veds.shared.web.security.KeycloakJwtAuthenticationConverter
import com.vertyll.veds.shared.web.security.ProblemAuthenticationEntryPoint
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
internal class SecurityConfig(
    private val keycloakJwtAuthenticationConverter: KeycloakJwtAuthenticationConverter,
    private val problemAuthenticationEntryPoint: ProblemAuthenticationEntryPoint,
) {
    private companion object {
        private val PUBLIC_ENDPOINTS =
            arrayOf(
                "/internal/**",
                "/translations/**",
                "/actuator/**",
                "/api-docs/**",
                "/v3/api-docs/**",
                "/swagger-ui/**",
                "/swagger-ui.html",
            )
    }

    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .csrf { it.disable() }
            .cors { it.disable() }
            .formLogin { it.disable() }
            .httpBasic { it.disable() }
            .authorizeHttpRequests { authorize ->
                authorize
                    .requestMatchers(*PUBLIC_ENDPOINTS)
                    .permitAll()
                    .anyRequest()
                    .authenticated()
            }.sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .oauth2ResourceServer { oauth2 ->
                oauth2
                    .authenticationEntryPoint(problemAuthenticationEntryPoint)
                    .jwt { jwt ->
                        jwt.jwtAuthenticationConverter(keycloakJwtAuthenticationConverter)
                    }
            }

        return http.build()
    }
}
