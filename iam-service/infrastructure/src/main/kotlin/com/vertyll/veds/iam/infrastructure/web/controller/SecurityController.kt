package com.vertyll.veds.iam.infrastructure.web.controller

import com.vertyll.veds.iam.application.dto.SecuritySettingsResponse
import com.vertyll.veds.iam.application.port.inbound.query.SecurityQueryUseCase
import com.vertyll.veds.iam.infrastructure.web.security.CurrentUser
import com.vertyll.veds.shared.web.security.ScopedToCaller
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/auth/me/security")
@Tag(name = "Account security", description = "Second-factor status")
@ScopedToCaller("the Keycloak subject in the token is the only account whose second factor this touches")
internal class SecurityController(
    private val securityQueries: SecurityQueryUseCase,
) {
    @GetMapping
    @Operation(summary = "Get the caller's second-factor status")
    fun getSettings(
        @AuthenticationPrincipal jwt: Jwt,
    ): ResponseEntity<SecuritySettingsResponse> {
        val settings = securityQueries.getSecuritySettings(CurrentUser.keycloakIdOf(jwt))
        return ResponseEntity.ok(settings)
    }
}
