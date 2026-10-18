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
            permission("USERS_VIEW", "View the user directory", RoleScope.GLOBAL)
            permission("USERS_MANAGE", "Edit users and assign roles", RoleScope.GLOBAL)
            permission("ROLES_VIEW", "View roles and their permissions", RoleScope.GLOBAL)
            permission("ROLES_MANAGE", "Create roles and change what they grant", RoleScope.GLOBAL)
        }
}
