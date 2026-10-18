package com.vertyll.veds.task.infrastructure.config

import com.vertyll.veds.sharedauthz.PermissionCatalogue
import com.vertyll.veds.sharedauthz.RoleScope
import com.vertyll.veds.sharedauthz.permissions
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
internal class PermissionCatalogueConfig {
    @Bean
    fun taskPermissionCatalogue(): PermissionCatalogue =
        permissions("task") {
            permission("VIEW_TASKS", "See the tasks of a project", RoleScope.PROJECT)
            permission("MANAGE_TASKS", "Create, edit and archive tasks", RoleScope.PROJECT)
            permission("COMMENT", "Comment on a task", RoleScope.PROJECT)
            permission("LOG_WORK", "Record time spent on a task", RoleScope.PROJECT)
            permission("VIEW_RESTRICTED_TASKS", "See tasks restricted to one role", RoleScope.PROJECT)
            permission("VIEW_HIDDEN_WORK_LOG", "Read work log entries somebody marked hidden", RoleScope.PROJECT)

            stockRole(
                "MANAGER",
                RoleScope.PROJECT,
                "VIEW_TASKS",
                "MANAGE_TASKS",
                "COMMENT",
                "LOG_WORK",
                "VIEW_RESTRICTED_TASKS",
                "VIEW_HIDDEN_WORK_LOG",
            )
            stockRole("MEMBER", RoleScope.PROJECT, "VIEW_TASKS", "MANAGE_TASKS", "COMMENT", "LOG_WORK")
            stockRole("CLIENT", RoleScope.PROJECT, "VIEW_TASKS", "COMMENT")
        }
}
