package com.vertyll.veds.iam.application.service.command

import com.vertyll.veds.iam.application.command.RegisterPermissionCatalogueCommand
import com.vertyll.veds.iam.application.command.RegisterPermissionCatalogueCommand.StockRoleDeclaration
import com.vertyll.veds.iam.application.port.inbound.command.PermissionCatalogueUseCase
import com.vertyll.veds.iam.application.port.outbound.RolePermissionsEventPublisherPort
import com.vertyll.veds.iam.domain.model.Permission
import com.vertyll.veds.iam.domain.model.Role
import com.vertyll.veds.iam.domain.repository.PermissionRepository
import com.vertyll.veds.iam.domain.repository.RoleRepository

class PermissionCatalogueService(
    private val permissionRepository: PermissionRepository,
    private val roleRepository: RoleRepository,
    private val eventPublisher: RolePermissionsEventPublisherPort,
) : PermissionCatalogueUseCase {
    override fun register(command: RegisterPermissionCatalogueCommand) {
        val stored = permissionRepository.findByModule(command.module).associateBy { it.name }
        val firstSeen = storeDeclared(command, stored)

        command.stockRoles.forEach { declaration -> applyStockGrants(declaration, firstSeen) }
        withdrawUndeclared(command, stored)
        announceEveryRole()
    }

    private fun announceEveryRole() {
        roleRepository.findAll().forEach(eventPublisher::publishChanged)
    }

    private fun storeDeclared(
        command: RegisterPermissionCatalogueCommand,
        stored: Map<String, Permission>,
    ): Set<String> {
        val firstSeen = mutableSetOf<String>()

        command.permissions.forEach { declaration ->
            val existing = stored[declaration.name]
            when {
                existing == null -> {
                    permissionRepository.save(
                        Permission.create(
                            name = declaration.name,
                            module = command.module,
                            scope = declaration.scope,
                            description = declaration.description,
                        ),
                    )
                    firstSeen += declaration.name
                }

                existing.description != declaration.description || existing.scope != declaration.scope -> {
                    permissionRepository.save(
                        existing.copy(description = declaration.description, scope = declaration.scope),
                    )
                }
            }
        }

        return firstSeen
    }

    private fun applyStockGrants(
        declaration: StockRoleDeclaration,
        firstSeen: Set<String>,
    ) {
        val existing = roleRepository.findByName(declaration.name)

        if (existing == null) {
            val created =
                roleRepository.save(
                    Role.create(
                        name = declaration.name,
                        description = "iam.role.shipped.description",
                        permissions = permissionRepository.findAllByNames(declaration.permissions).toSet(),
                        scope = declaration.scope,
                    ),
                )
            eventPublisher.publishChanged(created)
            return
        }

        val added = permissionRepository.findAllByNames(declaration.permissions.intersect(firstSeen))
        if (added.isEmpty()) return

        val updated = roleRepository.save(existing.withPermissions(existing.permissions + added))
        eventPublisher.publishChanged(updated)
    }

    private fun withdrawUndeclared(
        command: RegisterPermissionCatalogueCommand,
        stored: Map<String, Permission>,
    ) {
        val declared = command.permissions.map { it.name }.toSet()
        val withdrawn = stored.filterKeys { it !in declared }.values
        if (withdrawn.isEmpty()) return

        val withdrawnNames = withdrawn.map { it.name }.toSet()
        val affected = roleRepository.findAll().filter { role -> role.permissions.any { it.name in withdrawnNames } }

        withdrawn.forEach(permissionRepository::delete)

        affected.forEach { role ->
            val kept = role.permissions.filterNot { it.name in withdrawnNames }.toSet()
            eventPublisher.publishChanged(roleRepository.save(role.withPermissions(kept)))
        }
    }
}
