package com.vertyll.veds.notification.domain.model

enum class NotificationType(
    val key: String,
) {
    PROJECT_INVITATION("notification.project_invitation"),
    PROJECT_MEMBER_JOINED("notification.project_member_joined"),
    TASK_CREATED("notification.task_created"),
    TASK_ASSIGNED("notification.task_assigned"),
    TASK_STATUS_CHANGED("notification.task_status_changed"),
    TASK_COMMENT_ADDED("notification.task_comment_added"),
}
