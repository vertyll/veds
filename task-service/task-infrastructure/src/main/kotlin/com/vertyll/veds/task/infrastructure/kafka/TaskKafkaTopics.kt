package com.vertyll.veds.task.infrastructure.kafka

internal object TaskKafkaTopics {
    const val TASK_CREATED = "task-created"
    const val TASK_ASSIGNED = "task-assigned"
    const val TASK_STATUS_CHANGED = "task-status-changed"
    const val TASK_ARCHIVED = "task-archived"
    const val TASK_COMMENT_ADDED = "task-comment-added"

    object Consumed {
        const val PROJECT_CREATED = "project-created"
        const val PROJECT_UPDATED = "project-updated"
        const val PROJECT_ARCHIVED = "project-archived"
        const val PROJECT_MEMBER_JOINED = "project-member-joined"
        const val PROJECT_MEMBER_REMOVED = "project-member-removed"
        const val PROJECT_CATEGORY_CHANGED = "project-category-changed"
        const val PROJECT_STATUS_CHANGED = "project-status-changed"
        const val ROLE_PERMISSIONS_CHANGED = "role-permissions-changed"
    }
}
