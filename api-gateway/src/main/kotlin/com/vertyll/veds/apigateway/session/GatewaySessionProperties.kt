package com.vertyll.veds.apigateway.session

import jakarta.validation.constraints.NotBlank
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.validation.annotation.Validated

@ConfigurationProperties(prefix = "application.gateway.session")
@Validated
data class GatewaySessionProperties(
    @field:NotBlank
    val encryptionKey: String,
)
