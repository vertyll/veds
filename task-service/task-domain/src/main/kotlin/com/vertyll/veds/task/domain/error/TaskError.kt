package com.vertyll.veds.task.domain.error

import com.vertyll.veds.sharederror.DomainError
import com.vertyll.veds.sharederror.ErrorKind

enum class TaskError(
    override val key: String,
    override val kind: ErrorKind,
) : DomainError {
    TASK_NOT_FOUND("task.not_found", ErrorKind.NOT_FOUND),
    TASK_ARCHIVED("task.archived", ErrorKind.CONFLICT),
    TASK_ACCESS_DENIED("task.access_denied", ErrorKind.ACCESS_DENIED),

    COMMENT_NOT_FOUND("task.comment.not_found", ErrorKind.NOT_FOUND),
    COMMENT_NOT_AUTHORED_BY_CALLER("task.comment.not_authored_by_caller", ErrorKind.ACCESS_DENIED),

    WORK_LOG_NOT_FOUND("task.work_log.not_found", ErrorKind.NOT_FOUND),
    WORK_LOG_NOT_AUTHORED_BY_CALLER("task.work_log.not_authored_by_caller", ErrorKind.ACCESS_DENIED),

    PROJECT_NOT_KNOWN("task.project.not_known", ErrorKind.NOT_FOUND),
    PROJECT_ARCHIVED("task.project.archived", ErrorKind.CONFLICT),
    STATUS_NOT_IN_PROJECT("task.status.not_in_project", ErrorKind.INVALID),
    CATEGORY_NOT_IN_PROJECT("task.category.not_in_project", ErrorKind.INVALID),
    ASSIGNEE_NOT_A_MEMBER("task.assignee.not_a_member", ErrorKind.INVALID),
    INVALID_WORK_LOG_VISIBILITY("task.work_log.invalid_visibility", ErrorKind.INVALID),

    VERSION_MISMATCH("common.version_mismatch", ErrorKind.PRECONDITION_FAILED),
    LANGUAGE_NOT_SUPPLIED("common.language_not_supplied", ErrorKind.INVALID),
    LANGUAGE_NOT_SUPPORTED("common.language_not_supported", ErrorKind.INVALID),
    TOKEN_CLAIM_MISSING("common.token_claim_missing", ErrorKind.ACCESS_DENIED),
}
