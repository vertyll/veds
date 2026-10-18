package com.vertyll.veds.iam.infrastructure.web.controller

import com.vertyll.veds.iam.application.dto.UserResponse
import com.vertyll.veds.iam.application.port.inbound.command.ProvisionCurrentUserUseCase
import com.vertyll.veds.iam.application.port.inbound.query.AuthQueryUseCase
import com.vertyll.veds.iam.application.port.inbound.query.UserQueryUseCase
import com.vertyll.veds.iam.infrastructure.web.security.CurrentUser
import com.vertyll.veds.shared.web.http.ETagUtils
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
@RequestMapping("/auth")
@Tag(name = "Authentication", description = "Who is calling, and what this installation says they may do")
@ScopedToCaller("the Keycloak subject in the token is the only account this reads or provisions")
internal class AuthController(
    private val authServiceQueries: AuthQueryUseCase,
    private val provisionCurrentUser: ProvisionCurrentUserUseCase,
    private val userServiceQueries: UserQueryUseCase,
) {
    @GetMapping("/me")
    @Operation(summary = "Get the signed-in person's profile")
    fun getCurrentUser(
        @AuthenticationPrincipal jwt: Jwt,
    ): ResponseEntity<UserResponse> {
        val keycloakId = CurrentUser.keycloakIdOf(jwt)
        provisionCurrentUser.provision(CurrentUser.identityOf(jwt))

        val user = userServiceQueries.getUserByKeycloakId(keycloakId)
        val etag = ETagUtils.buildWeakETag(user.version) ?: return ResponseEntity.ok(user)
        return ResponseEntity.ok().eTag(etag).body(user)
    }

    @GetMapping("/me/permissions")
    @Operation(summary = "Get current user permissions from local database")
    fun getCurrentUserPermissions(
        @AuthenticationPrincipal jwt: Jwt,
    ): ResponseEntity<List<String>> {
        val keycloakId = CurrentUser.keycloakIdOf(jwt)
        val permissions = authServiceQueries.getUserPermissions(keycloakId)

        return ResponseEntity.ok(permissions)
    }
}
