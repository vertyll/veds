package com.vertyll.veds.shared.web.error

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.bind.annotation.RestControllerAdvice

/**
 * Registers the shared exception handler, so a service gets consistent error
 * responses without scanning this package or declaring anything.
 */
@Configuration
@ConditionalOnClass(RestControllerAdvice::class)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
class ErrorHandlingAutoConfiguration {
    /** The handler every service shares, unless it defines its own. */
    @Bean
    @ConditionalOnMissingBean
    fun sharedGlobalExceptionHandler(): GlobalExceptionHandler = GlobalExceptionHandler()
}
