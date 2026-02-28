package com.vertyll.veds.apigateway.config

import org.slf4j.LoggerFactory
import org.springframework.cloud.gateway.filter.GatewayFilterChain
import org.springframework.cloud.gateway.filter.GlobalFilter
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.Ordered
import org.springframework.http.HttpHeaders
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono

@Configuration
class GatewayConfig {
    @Bean
    fun customGlobalFilter(): GlobalFilter = HeadersExchangeFilter()

    private class HeadersExchangeFilter :
        GlobalFilter,
        Ordered {
        private val log = LoggerFactory.getLogger(HeadersExchangeFilter::class.java)

        override fun getOrder(): Int = Ordered.HIGHEST_PRECEDENCE

        @Suppress("kotlin:S6508")
        override fun filter(
            exchange: ServerWebExchange,
            chain: GatewayFilterChain,
        ): Mono<Void> {
            val request = exchange.request
            val authorization = request.headers.getFirst(HttpHeaders.AUTHORIZATION)

            if (authorization != null) {
                log.debug("Propagating Authorization header to downstream service: {}", request.path)
            } else {
                log.debug("No Authorization header found for path: {}", request.path)
            }

            return chain.filter(exchange)
        }
    }
}
