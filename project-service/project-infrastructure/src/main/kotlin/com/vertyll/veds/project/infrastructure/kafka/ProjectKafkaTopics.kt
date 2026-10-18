package com.vertyll.veds.project.infrastructure.kafka

internal object ProjectKafkaTopics {
    const val PROJECT_CREATED = "project-created"
    const val PROJECT_UPDATED = "project-updated"
    const val PROJECT_ARCHIVED = "project-archived"
    const val PROJECT_MEMBER_INVITED = "project-member-invited"
    const val PROJECT_MEMBER_JOINED = "project-member-joined"
    const val PROJECT_MEMBER_REMOVED = "project-member-removed"
    const val PROJECT_CATEGORY_CHANGED = "project-category-changed"
    const val PROJECT_STATUS_CHANGED = "project-status-changed"

    object Consumed {
        const val ROLE_PERMISSIONS_CHANGED = "role-permissions-changed"
    }
}
