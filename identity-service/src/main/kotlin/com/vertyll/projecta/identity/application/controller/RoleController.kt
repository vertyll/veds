package com.vertyll.projecta.identity.application.controller

import com.vertyll.projecta.identity.domain.dto.RoleResponseDto
import com.vertyll.projecta.identity.domain.service.RoleService
import com.vertyll.projecta.identity.infrastructure.response.ApiResponse
import com.vertyll.projecta.sharedinfrastructure.http.ETagUtil
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/roles")
@Tag(name = "Roles", description = "Role management APIs")
class RoleController(
    private val roleService: RoleService,
) {
    private companion object {
        private const val ROLE_RETRIEVED_SUCCESSFULLY = "Role retrieved successfully"
        private const val USER_ROLES_RETRIEVED_SUCCESSFULLY = "User roles retrieved successfully"
        private const val ROLE_ASSIGNED_SUCCESSFULLY = "Role assigned successfully"
        private const val ROLE_REMOVED_SUCCESSFULLY = "Role removed successfully"
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get role by ID")
    fun getRoleById(
        @PathVariable id: Long,
    ): ResponseEntity<ApiResponse<RoleResponseDto>> {
        val role = roleService.getRoleById(id)
        val etag = ETagUtil.buildWeakETag(role.version)
        val response =
            ApiResponse.buildResponse(
                data = role,
                message = ROLE_RETRIEVED_SUCCESSFULLY,
                status = HttpStatus.OK,
            )
        return if (etag != null) ResponseEntity.status(HttpStatus.OK).eTag(etag).body(response.body) else response
    }

    @GetMapping("/name/{name}")
    @Operation(summary = "Get role by name")
    fun getRoleByName(
        @PathVariable name: String,
    ): ResponseEntity<ApiResponse<RoleResponseDto>> {
        val role = roleService.getRoleByName(name)
        val etag = ETagUtil.buildWeakETag(role.version)
        val response =
            ApiResponse.buildResponse(
                data = role,
                message = ROLE_RETRIEVED_SUCCESSFULLY,
                status = HttpStatus.OK,
            )
        return if (etag != null) ResponseEntity.status(HttpStatus.OK).eTag(etag).body(response.body) else response
    }

    @GetMapping
    @Operation(summary = "Get all roles")
    fun getAllRoles(): ResponseEntity<ApiResponse<List<RoleResponseDto>>> {
        val roles = roleService.getAllRoles()
        val response =
            ApiResponse.buildResponse(
                data = roles,
                message = ROLE_RETRIEVED_SUCCESSFULLY,
                status = HttpStatus.OK,
            )
        return response
    }

    @GetMapping("/user/{userId}")
    @Operation(summary = "Get roles for a user")
    fun getRolesForUser(
        @PathVariable userId: Long,
    ): ResponseEntity<ApiResponse<List<RoleResponseDto>>> {
        val roles = roleService.getRolesForUser(userId)
        val response =
            ApiResponse.buildResponse(
                data = roles,
                message = USER_ROLES_RETRIEVED_SUCCESSFULLY,
                status = HttpStatus.OK,
            )
        return response
    }

    @PostMapping("/user/{userId}/role/{roleName}")
    @Operation(summary = "Assign a role to a user")
    fun assignRoleToUser(
        @PathVariable userId: Long,
        @PathVariable roleName: String,
        @RequestHeader(HttpHeaders.IF_MATCH, required = false) ifMatch: String?,
    ): ResponseEntity<ApiResponse<Any>> {
        val version = ETagUtil.parseIfMatchToVersion(ifMatch)
        roleService.assignRoleToUser(userId, roleName, version)
        return ApiResponse.buildResponse(
            data = null,
            message = ROLE_ASSIGNED_SUCCESSFULLY,
            status = HttpStatus.OK,
        )
    }

    @DeleteMapping("/user/{userId}/role/{roleName}")
    @Operation(summary = "Remove a role from a user")
    fun removeRoleFromUser(
        @PathVariable userId: Long,
        @PathVariable roleName: String,
        @RequestHeader(HttpHeaders.IF_MATCH, required = false) ifMatch: String?,
    ): ResponseEntity<ApiResponse<Any>> {
        val version = ETagUtil.parseIfMatchToVersion(ifMatch)
        roleService.removeRoleFromUser(userId, roleName, version)
        return ApiResponse.buildResponse(
            data = null,
            message = ROLE_REMOVED_SUCCESSFULLY,
            status = HttpStatus.OK,
        )
    }
}
