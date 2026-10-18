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
                        catalogue.definitions.map {
                            RegisterPermissionCatalogueCommand.PermissionDeclaration(
                                name = it.name,
                                description = it.description,
                                scope = RoleScope.valueOf(it.scope.name),
                            )
                        },
                    stockRoles =
                        catalogue.stockRoles.map {
                            RegisterPermissionCatalogueCommand.StockRoleDeclaration(
                                name = it.name,
                                scope = RoleScope.valueOf(it.scope.name),
                                permissions = it.permissions,
                            )
                        },
                ),
            )
            logger.info("Registered {} permissions for module {}", catalogue.definitions.size, catalogue.module)
        }
    }
}
