package com.vertyll.projecta.mail.infrastructure.security

import com.vertyll.projecta.sharedinfrastructure.config.JwtConstants
import com.vertyll.projecta.sharedinfrastructure.config.SharedConfigProperties
import io.jsonwebtoken.Claims
import io.jsonwebtoken.JwtException
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.io.Decoders
import io.jsonwebtoken.security.Keys
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
import javax.crypto.SecretKey

@Component
class JwtAuthenticationFilter(
    private val sharedConfig: SharedConfigProperties,
) : OncePerRequestFilter() {
    private val log = LoggerFactory.getLogger(JwtAuthenticationFilter::class.java)

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        val authHeader = request.getHeader(sharedConfig.security.jwt.authHeaderName)

        if (authHeader == null || !authHeader.startsWith(JwtConstants.BEARER_PREFIX)) {
            filterChain.doFilter(request, response)
            return
        }

        try {
            val jwt = authHeader.substring(JwtConstants.BEARER_PREFIX.length)
            val username = extractUsername(jwt)

            if (username.isNotBlank() && SecurityContextHolder.getContext().authentication == null) {
                val roles = extractRoles(jwt)
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

                log.debug("Successfully authenticated user: {} with roles: {}", username, roles)
            }
        } catch (e: JwtException) {
            log.debug("Invalid JWT token: {}", e.message)
        } catch (e: Exception) {
            log.error("JWT authentication failed: {}", e.message, e)
        }

        filterChain.doFilter(request, response)
    }

    private fun extractUsername(token: String): String = extractClaim(token) { it.subject }

    private fun extractRoles(token: String): List<String> =
        extractClaim(token) { claims ->
            @Suppress("UNCHECKED_CAST")
            claims[JwtConstants.CLAIM_ROLES] as? List<String> ?: emptyList()
        }

    private fun <T> extractClaim(
        token: String,
        claimsResolver: (Claims) -> T,
    ): T {
        val claims = extractAllClaims(token)
        return claimsResolver(claims)
    }

    private fun extractAllClaims(token: String): Claims =
        Jwts
            .parser()
            .verifyWith(getSigningKey())
            .build()
            .parseSignedClaims(token)
            .payload

    private fun getSigningKey(): SecretKey {
        val keyBytes = Decoders.BASE64.decode(sharedConfig.security.jwt.secretKey)
        return Keys.hmacShaKeyFor(keyBytes)
    }
}
