package com.vertyll.veds.iam.infrastructure.config

import com.vertyll.veds.iam.application.command.RegisterPermissionCatalogueCommand
import com.vertyll.veds.iam.application.port.inbound.command.PermissionCatalogueUseCase
import com.vertyll.veds.iam.domain.model.RoleScope
import com.vertyll.veds.sharedauthz.PermissionCatalogue
import org.slf4j.LoggerFactory
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component

@Component
@Order(0)
internal class PermissionCatalogueRegistrar(
    private val catalogues: List<PermissionCatalogue>,
    private val permissionCatalogue: PermissionCatalogueUseCase,
) : ApplicationRunner {
    private val logger = LoggerFactory.getLogger(javaClass)

    override fun run(args: ApplicationArguments) {
        catalogues.forEach { catalogue ->
            permissionCatalogue.register(
                RegisterPermissionCatalogueCommand(
                    module = catalogue.module,
                    permissions =
                        catalogue.definitions.map { definition ->
                            RegisterPermissionCatalogueCommand.PermissionDeclaration(
                                name = definition.name,
                                description = definition.description,
                                scope = RoleScope.valueOf(definition.scope.name),
                            )
                        },
                    stockRoles =
                        catalogue.stockRoles.map { role ->
                            RegisterPermissionCatalogueCommand.StockRoleDeclaration(
                                name = role.name,
                                scope = RoleScope.valueOf(role.scope.name),
                                permissions = role.permissions,
                            )
                        },
                ),
            )
            logger.info("Registered {} permissions for module {}", catalogue.definitions.size, catalogue.module)
        }
    }
}
