package com.vertyll.veds.iam.application.port.inbound.command

import com.vertyll.veds.iam.application.command.RegisterPermissionCatalogueCommand

@Suppress("kotlin:S6517")
interface PermissionCatalogueUseCase {
    fun register(command: RegisterPermissionCatalogueCommand)
}
