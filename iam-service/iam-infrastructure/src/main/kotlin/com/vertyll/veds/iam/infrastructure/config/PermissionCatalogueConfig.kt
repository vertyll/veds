package com.vertyll.veds.iam.infrastructure.config

import com.vertyll.veds.sharedauthz.PermissionCatalogue
import com.vertyll.veds.sharedauthz.RoleScope
import com.vertyll.veds.sharedauthz.permissions
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
internal class PermissionCatalogueConfig {
    @Bean
    fun adminPermissionCatalogue(): PermissionCatalogue =
        permissions("admin") {
            permission(name = "USERS_VIEW", description = "View the user directory", scope = RoleScope.GLOBAL)
            permission(name = "USERS_MANAGE", description = "Edit users and assign roles", scope = RoleScope.GLOBAL)
            permission(name = "ROLES_VIEW", description = "View roles and their permissions", scope = RoleScope.GLOBAL)
            permission(name = "ROLES_MANAGE", description = "Create roles and change what they grant", scope = RoleScope.GLOBAL)
        }
}
