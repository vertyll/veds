package com.vertyll.veds.iam.infrastructure.web.controller

import com.vertyll.veds.iam.application.dto.UserResponse
import com.vertyll.veds.iam.application.port.inbound.command.UserCommandUseCase
import com.vertyll.veds.iam.application.port.inbound.query.UserQueryUseCase
import com.vertyll.veds.iam.domain.model.PageRequest
import com.vertyll.veds.iam.domain.model.PageResult
import com.vertyll.veds.iam.infrastructure.web.dto.UpdateProfileRequest
import com.vertyll.veds.iam.infrastructure.web.security.CurrentUser
import com.vertyll.veds.shared.web.http.ETagUtils
import com.vertyll.veds.shared.web.security.ScopedToCaller
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/users")
@Tag(name = "Users", description = "User management API")
internal class UserController(
    private val userServiceCommands: UserCommandUseCase,
    private val userServiceQueries: UserQueryUseCase,
) {
    @GetMapping
    @PreAuthorize("@authz.has('USERS_VIEW')")
    @Operation(summary = "Get all users")
    fun getAllUsers(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
        @RequestParam(required = false) searchTerm: String?,
    ): ResponseEntity<PageResult<UserResponse>> {
        val users = userServiceQueries.searchUsers(searchTerm.orEmpty(), PageRequest(page = page, size = size))
        return ResponseEntity.ok(users)
    }

    @PreAuthorize("@authz.has('USERS_VIEW')")
    @GetMapping("/{id}")
    @Operation(summary = "Get user by ID")
    fun getUserById(
        @PathVariable id: Long,
    ): ResponseEntity<UserResponse> {
        val user = userServiceQueries.getUserById(id)
        val etag = ETagUtils.buildWeakETag(user.version)
        val response = ResponseEntity.ok(user)
        return if (etag != null) ResponseEntity.status(HttpStatus.OK).eTag(etag).body(response.body) else response
    }

    @PreAuthorize("@authz.has('USERS_VIEW')")
    @GetMapping("/email/{email}")
    @Operation(summary = "Get user by email")
    fun getUserByEmail(
        @PathVariable email: String,
    ): ResponseEntity<UserResponse> {
        val user = userServiceQueries.getUserByEmail(email)
        val etag = ETagUtils.buildWeakETag(user.version)
        val response = ResponseEntity.ok(user)
        return if (etag != null) ResponseEntity.status(HttpStatus.OK).eTag(etag).body(response.body) else response
    }

    @PutMapping("/me")
    @ScopedToCaller("the Keycloak subject in the token names the profile being changed")
    @Operation(summary = "Update your own profile")
    fun updateOwnProfile(
        @AuthenticationPrincipal jwt: Jwt,
        @Valid @RequestBody request: UpdateProfileRequest,
        @RequestHeader(HttpHeaders.IF_MATCH, required = false) ifMatch: String?,
    ): ResponseEntity<UserResponse> {
        val version = ETagUtils.parseIfMatchToVersion(ifMatch)
        val me = userServiceQueries.getUserByKeycloakId(CurrentUser.keycloakIdOf(jwt))
        val user = userServiceCommands.updateProfile(me.id, request.toCommand(), version)
        val etag = ETagUtils.buildWeakETag(user.version)
        val response = ResponseEntity.ok(user)
        return if (etag != null) ResponseEntity.status(HttpStatus.OK).eTag(etag).body(response.body) else response
    }

    @PreAuthorize("@authz.has('USERS_MANAGE')")
    @PutMapping("/{id}")
    @Operation(summary = "Update somebody else's profile")
    fun updateProfile(
        @PathVariable id: Long,
        @Valid @RequestBody request: UpdateProfileRequest,
        @RequestHeader(HttpHeaders.IF_MATCH, required = false) ifMatch: String?,
    ): ResponseEntity<UserResponse> {
        val version = ETagUtils.parseIfMatchToVersion(ifMatch)
        val user = userServiceCommands.updateProfile(id, request.toCommand(), version)
        val etag = ETagUtils.buildWeakETag(user.version)
        val response = ResponseEntity.ok(user)
        return if (etag != null) ResponseEntity.status(HttpStatus.OK).eTag(etag).body(response.body) else response
    }
}
