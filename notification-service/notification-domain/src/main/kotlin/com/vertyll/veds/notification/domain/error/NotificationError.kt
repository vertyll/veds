package com.vertyll.veds.notification.domain.error

import com.vertyll.veds.sharederror.DomainError
import com.vertyll.veds.sharederror.ErrorKind

enum class NotificationError(
    override val key: String,
    override val kind: ErrorKind,
) : DomainError {
    NOTIFICATION_NOT_FOUND("notification.not_found", ErrorKind.NOT_FOUND),

    VERSION_MISMATCH("common.version_mismatch", ErrorKind.PRECONDITION_FAILED),
    TOKEN_CLAIM_MISSING("common.token_claim_missing", ErrorKind.ACCESS_DENIED),
}
