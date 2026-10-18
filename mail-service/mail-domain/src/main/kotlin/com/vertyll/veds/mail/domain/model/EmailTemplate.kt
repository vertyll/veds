package com.vertyll.veds.mail.domain.model

enum class EmailTemplate(
    val templateName: String,
    val subject: String,
) {
    PROJECT_INVITATION("PROJECT_INVITATION", "You have been invited to a project"),
    PROJECT_MEMBER_JOINED("PROJECT_MEMBER_JOINED", "New project member"),
    TASK_CREATED("TASK_CREATED", "New task"),
    TASK_ASSIGNED("TASK_ASSIGNED", "A task was assigned to you"),
    TASK_STATUS_CHANGED("TASK_STATUS_CHANGED", "Task status changed"),
    TASK_COMMENT_ADDED("TASK_COMMENT_ADDED", "New comment"),
    ;

    companion object {
        fun fromTemplateName(name: String): EmailTemplate? = EmailTemplate.entries.find { it.templateName == name }
    }
}
