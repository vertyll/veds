package com.vertyll.veds.iam.infrastructure.web.controller

import com.vertyll.veds.iam.application.command.RegisterPermissionCatalogueCommand
import com.vertyll.veds.iam.application.port.inbound.command.PermissionCatalogueUseCase
import com.vertyll.veds.iam.domain.model.RoleScope
import com.vertyll.veds.iam.infrastructure.web.dto.RegisterPermissionCatalogueRequest
import com.vertyll.veds.shared.web.security.PublicEndpoint
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/internal/authz")
@Tag(name = "Permission catalogue", description = "Registration of the permissions a module checks")
@PublicEndpoint("service-to-service registration inside the cluster; the gateway routes no /internal path and NetworkPolicy keeps it there")
internal class PermissionCatalogueController(
    private val catalogue: PermissionCatalogueUseCase,
) {
    @Suppress("kotlin:S6508")
    @PostMapping("/catalogue")
    @Operation(summary = "Register the permissions a module declares")
    fun register(
        @Valid @RequestBody request: RegisterPermissionCatalogueRequest,
    ): ResponseEntity<Void> {
        catalogue.register(
            RegisterPermissionCatalogueCommand(
                module = request.module,
                permissions =
                    request.permissions.map {
                        RegisterPermissionCatalogueCommand.PermissionDeclaration(
                            name = it.name,
                            description = it.description,
                            scope = RoleScope.fromString(it.scope) ?: error("unknown permission scope '${it.scope}'"),
                        )
                    },
                stockRoles =
                    request.stockRoles.map {
                        RegisterPermissionCatalogueCommand.StockRoleDeclaration(
                            name = it.name,
                            scope = RoleScope.fromString(it.scope) ?: error("unknown role scope '${it.scope}'"),
                            permissions = it.permissions,
                        )
                    },
            ),
        )
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build()
    }
}
