package com.vertyll.veds.task.infrastructure.config

import com.vertyll.veds.sharedauthz.PermissionCatalogue
import com.vertyll.veds.sharedauthz.RoleScope
import com.vertyll.veds.sharedauthz.permissions
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

private const val VIEW_TASKS = "VIEW_TASKS"
private const val MANAGE_TASKS = "MANAGE_TASKS"
private const val COMMENT = "COMMENT"
private const val LOG_WORK = "LOG_WORK"

@Configuration
internal class PermissionCatalogueConfig {
    @Bean
    fun taskPermissionCatalogue(): PermissionCatalogue =
        permissions("task") {
            permission(name = VIEW_TASKS, description = "See the tasks of a project", scope = RoleScope.PROJECT)
            permission(name = MANAGE_TASKS, description = "Create, edit and archive tasks", scope = RoleScope.PROJECT)
            permission(name = COMMENT, description = "Comment on a task", scope = RoleScope.PROJECT)
            permission(name = LOG_WORK, description = "Record time spent on a task", scope = RoleScope.PROJECT)
            permission(name = "VIEW_RESTRICTED_TASKS", description = "See tasks restricted to one role", scope = RoleScope.PROJECT)
            permission(
                name = "VIEW_HIDDEN_WORK_LOG",
                description = "Read work log entries somebody marked hidden",
                scope = RoleScope.PROJECT,
            )

            stockRole(
                "MANAGER",
                RoleScope.PROJECT,
                VIEW_TASKS,
                MANAGE_TASKS,
                COMMENT,
                LOG_WORK,
                "VIEW_RESTRICTED_TASKS",
                "VIEW_HIDDEN_WORK_LOG",
            )
            stockRole("MEMBER", RoleScope.PROJECT, VIEW_TASKS, MANAGE_TASKS, COMMENT, LOG_WORK)
            stockRole("CLIENT", RoleScope.PROJECT, VIEW_TASKS, COMMENT)
        }
}
