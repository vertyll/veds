package com.vertyll.veds.apigateway

import com.vertyll.veds.apigateway.config.GatewayCorsProperties
import com.vertyll.veds.apigateway.config.GatewayOAuthProperties
import com.vertyll.veds.apigateway.session.GatewaySessionProperties
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication

@SpringBootApplication
@EnableConfigurationProperties(GatewaySessionProperties::class, GatewayCorsProperties::class, GatewayOAuthProperties::class)
class ApiGatewayApplication

fun main(args: Array<String>) {
    runApplication<ApiGatewayApplication>(*args)
}
