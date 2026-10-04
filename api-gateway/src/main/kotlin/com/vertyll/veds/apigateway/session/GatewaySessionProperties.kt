package com.vertyll.veds.apigateway.session

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "application.gateway.session")
data class GatewaySessionProperties(
    val encryptionKey: String,
)
