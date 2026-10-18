package com.vertyll.veds.iam.infrastructure.web.controller

import com.vertyll.veds.iam.application.dto.PermissionModuleResponse
import com.vertyll.veds.iam.application.dto.PermissionResponse
import com.vertyll.veds.iam.application.port.inbound.query.PermissionQueryUseCase
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/permissions")
@Tag(name = "Permissions", description = "Platform permission catalogue")
@PreAuthorize("@authz.has('ROLES_VIEW')")
internal class PermissionController(
    private val permissionQueries: PermissionQueryUseCase,
) {
    @GetMapping
    @Operation(summary = "List every permission with the roles that grant it")
    fun getPermissions(): ResponseEntity<List<PermissionResponse>> = ResponseEntity.ok(permissionQueries.getAllPermissions())

    @GetMapping("/modules")
    @Operation(summary = "List every permission grouped by the module that declares it")
    fun getPermissionsByModule(): ResponseEntity<List<PermissionModuleResponse>> =
        ResponseEntity.ok(permissionQueries.getPermissionsByModule())
}
