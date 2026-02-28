package com.vertyll.veds.apigateway.security

import com.vertyll.veds.sharedinfrastructure.config.JwtConstants
import com.vertyll.veds.sharedinfrastructure.config.SharedConfigProperties
import io.jsonwebtoken.Claims
import io.jsonwebtoken.JwtException
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.io.Decoders
import io.jsonwebtoken.security.Keys
import org.slf4j.LoggerFactory
import org.springframework.http.server.reactive.ServerHttpRequest
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.ReactiveSecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.server.WebFilter
import org.springframework.web.server.WebFilterChain
import reactor.core.publisher.Mono
import javax.crypto.SecretKey

@Component
class JwtAuthFilter(
    private val sharedConfig: SharedConfigProperties,
) : WebFilter {
    private val logger = LoggerFactory.getLogger(JwtAuthFilter::class.java)

    @Suppress("kotlin:S6508")
    override fun filter(
        exchange: ServerWebExchange,
        chain: WebFilterChain,
    ): Mono<Void> {
        val token = extractTokenFromRequest(exchange.request) ?: return chain.filter(exchange)

        return try {
            val claims = extractAllClaims(token)
            val username = claims.subject

            // Extract roles from token
            @Suppress("UNCHECKED_CAST")
            val roles = claims[JwtConstants.CLAIM_ROLES] as? List<String> ?: emptyList()

            // Create authorities from roles
            val authorities = roles.map { SimpleGrantedAuthority(it) }

            // Create authentication
            val authentication =
                UsernamePasswordAuthenticationToken(
                    username,
                    null,
                    authorities,
                )

            // Add authentication to the security context
            chain
                .filter(exchange)
                .contextWrite(ReactiveSecurityContextHolder.withAuthentication(authentication))
        } catch (ex: JwtException) {
            logger.debug("Invalid JWT token: {}", ex.message)
            chain.filter(exchange)
        } catch (ex: Exception) {
            logger.error("Error processing JWT token", ex)
            chain.filter(exchange)
        }
    }

    private fun extractTokenFromRequest(request: ServerHttpRequest): String? {
        val authHeaderName = sharedConfig.security.jwt.authHeaderName
        val authHeader = request.headers.getFirst(authHeaderName) ?: return null

        if (!authHeader.startsWith(JwtConstants.BEARER_PREFIX)) {
            return null
        }

        return authHeader.substring(JwtConstants.BEARER_PREFIX.length)
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
