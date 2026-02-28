package com.vertyll.veds.iam.infrastructure.config

import com.vertyll.veds.iam.infrastructure.security.JwtAuthenticationFilter
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter

@Configuration
@EnableWebSecurity
class SecurityConfig(
    private val jwtAuthenticationFilter: JwtAuthenticationFilter,
) {
    companion object {
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
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter::class.java)

        return http.build()
    }
}
