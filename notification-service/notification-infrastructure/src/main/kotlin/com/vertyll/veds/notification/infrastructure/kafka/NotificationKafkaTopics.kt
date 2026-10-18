package com.vertyll.veds.notification.infrastructure.kafka

internal object NotificationKafkaTopics {
    const val MAIL_REQUESTED = "mail-requested"

    object Consumed {
        const val PROJECT_MEMBER_INVITED = "project-member-invited"
        const val PROJECT_MEMBER_JOINED = "project-member-joined"

        const val TASK_CREATED = "task-created"
        const val TASK_ASSIGNED = "task-assigned"
        const val TASK_STATUS_CHANGED = "task-status-changed"
        const val TASK_COMMENT_ADDED = "task-comment-added"
        const val TASK_ARCHIVED = "task-archived"
    }
}
