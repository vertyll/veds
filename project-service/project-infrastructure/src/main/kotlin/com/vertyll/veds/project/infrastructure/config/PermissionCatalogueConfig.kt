package com.vertyll.veds.project.infrastructure.config

import com.vertyll.veds.sharedauthz.PermissionCatalogue
import com.vertyll.veds.sharedauthz.RoleScope
import com.vertyll.veds.sharedauthz.permissions
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

private const val VIEW_PROJECT = "VIEW_PROJECT"

@Configuration
internal class PermissionCatalogueConfig {
    @Bean
    fun projectPermissionCatalogue(): PermissionCatalogue =
        permissions("project") {
            permission(name = VIEW_PROJECT, description = "See a project and its members", scope = RoleScope.PROJECT)
            permission(name = "EDIT_PROJECT", description = "Change a project's settings", scope = RoleScope.PROJECT)
            permission(name = "DELETE_PROJECT", description = "Archive a project", scope = RoleScope.PROJECT)
            permission(name = "INVITE_USERS", description = "Invite people to a project", scope = RoleScope.PROJECT)
            permission(name = "MANAGE_MEMBERS", description = "Change what role a member holds", scope = RoleScope.PROJECT)

            stockRole("MANAGER", RoleScope.PROJECT, VIEW_PROJECT, "EDIT_PROJECT", "DELETE_PROJECT", "INVITE_USERS", "MANAGE_MEMBERS")
            stockRole("MEMBER", RoleScope.PROJECT, VIEW_PROJECT)
            stockRole("CLIENT", RoleScope.PROJECT, VIEW_PROJECT)
        }
}
