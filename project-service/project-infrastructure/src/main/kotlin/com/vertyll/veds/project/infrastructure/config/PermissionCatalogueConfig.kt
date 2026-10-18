package com.vertyll.veds.project.infrastructure.config

import com.vertyll.veds.sharedauthz.PermissionCatalogue
import com.vertyll.veds.sharedauthz.RoleScope
import com.vertyll.veds.sharedauthz.permissions
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
internal class PermissionCatalogueConfig {
    @Bean
    fun projectPermissionCatalogue(): PermissionCatalogue =
        permissions("project") {
            permission("VIEW_PROJECT", "See a project and its members", RoleScope.PROJECT)
            permission("EDIT_PROJECT", "Change a project's settings", RoleScope.PROJECT)
            permission("DELETE_PROJECT", "Archive a project", RoleScope.PROJECT)
            permission("INVITE_USERS", "Invite people to a project", RoleScope.PROJECT)
            permission("MANAGE_MEMBERS", "Change what role a member holds", RoleScope.PROJECT)

            stockRole("MANAGER", RoleScope.PROJECT, "VIEW_PROJECT", "EDIT_PROJECT", "DELETE_PROJECT", "INVITE_USERS", "MANAGE_MEMBERS")
            stockRole("MEMBER", RoleScope.PROJECT, "VIEW_PROJECT")
            stockRole("CLIENT", RoleScope.PROJECT, "VIEW_PROJECT")
        }
}
