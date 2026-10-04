package com.vertyll.veds.apigateway.session

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "application.redis")
data class RedisKeyProperties(
    val keyPrefix: String,
)
