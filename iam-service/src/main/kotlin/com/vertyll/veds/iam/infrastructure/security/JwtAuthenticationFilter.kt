package com.vertyll.veds.iam.infrastructure.security

import com.vertyll.veds.iam.domain.service.JwtService
import com.vertyll.veds.sharedinfrastructure.config.JwtConstants
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.core.userdetails.User
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Component
class JwtAuthenticationFilter(
    private val jwtService: JwtService,
) : OncePerRequestFilter() {
    private val log = LoggerFactory.getLogger(JwtAuthenticationFilter::class.java)

    companion object {
        private const val AUTH_HEADER_NAME = "Authorization"
    }

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        val authHeader = request.getHeader(AUTH_HEADER_NAME)

        if (authHeader == null || !authHeader.startsWith(JwtConstants.BEARER_PREFIX)) {
            filterChain.doFilter(request, response)
            return
        }

        try {
            val jwt = authHeader.substring(JwtConstants.BEARER_PREFIX.length)
            val username = jwtService.extractUsername(jwt)

            if (username.isNotBlank() && SecurityContextHolder.getContext().authentication == null) {
                val roles = jwtService.extractRoles(jwt)
                val authorities = roles.map { SimpleGrantedAuthority(it) }

                val userDetails: UserDetails =
                    User
                        .builder()
                        .username(username)
                        .password("")
                        .authorities(authorities)
                        .build()

                val authToken =
                    UsernamePasswordAuthenticationToken(
                        userDetails,
                        null,
                        userDetails.authorities,
                    )

                authToken.details = WebAuthenticationDetailsSource().buildDetails(request)
                SecurityContextHolder.getContext().authentication = authToken

                if (log.isDebugEnabled) {
                    log.debug("Successfully authenticated user: $username with roles: $roles")
                }
            }
        } catch (e: Exception) {
            log.error("JWT authentication failed: ${e.message}", e)
        }

        filterChain.doFilter(request, response)
    }
}
