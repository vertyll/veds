package com.vertyll.veds.identity.application.controller

import com.vertyll.veds.identity.domain.dto.UpdateProfileRequestDto
import com.vertyll.veds.identity.domain.dto.UserResponseDto
import com.vertyll.veds.identity.domain.service.UserService
import com.vertyll.veds.identity.infrastructure.response.ApiResponse
import com.vertyll.veds.sharedinfrastructure.http.ETagUtil
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/users")
@Tag(name = "Users", description = "User management API")
class UserController(
    private val userService: UserService,
) {
    private companion object {
        private const val USER_RETRIEVED_SUCCESSFULLY = "User retrieved successfully"
        private const val PROFILE_UPDATED_SUCCESSFULLY = "Profile updated successfully"
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get user by ID")
    fun getUserById(
        @PathVariable id: Long,
    ): ResponseEntity<ApiResponse<UserResponseDto>> {
        val user = userService.getUserById(id)
        val etag = ETagUtil.buildWeakETag(user.version)
        val response =
            ApiResponse.buildResponse(
                data = user,
                message = USER_RETRIEVED_SUCCESSFULLY,
                status = HttpStatus.OK,
            )
        return if (etag != null) ResponseEntity.status(HttpStatus.OK).eTag(etag).body(response.body) else response
    }

    @GetMapping("/email/{email}")
    @Operation(summary = "Get user by email")
    fun getUserByEmail(
        @PathVariable email: String,
    ): ResponseEntity<ApiResponse<UserResponseDto>> {
        val user = userService.getUserByEmail(email)
        val etag = ETagUtil.buildWeakETag(user.version)
        val response =
            ApiResponse.buildResponse(
                data = user,
                message = USER_RETRIEVED_SUCCESSFULLY,
                status = HttpStatus.OK,
            )
        return if (etag != null) ResponseEntity.status(HttpStatus.OK).eTag(etag).body(response.body) else response
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update user profile")
    fun updateProfile(
        @PathVariable id: Long,
        @Valid @RequestBody request: UpdateProfileRequestDto,
        @RequestHeader(HttpHeaders.IF_MATCH, required = false) ifMatch: String?,
    ): ResponseEntity<ApiResponse<UserResponseDto>> {
        val version = ETagUtil.parseIfMatchToVersion(ifMatch)
        val user = userService.updateProfile(id, request, version)
        val etag = ETagUtil.buildWeakETag(user.version)
        val response =
            ApiResponse.buildResponse(
                data = user,
                message = PROFILE_UPDATED_SUCCESSFULLY,
                status = HttpStatus.OK,
            )
        return if (etag != null) ResponseEntity.status(HttpStatus.OK).eTag(etag).body(response.body) else response
    }
}
