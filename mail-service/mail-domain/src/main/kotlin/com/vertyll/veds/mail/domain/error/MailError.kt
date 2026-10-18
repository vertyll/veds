package com.vertyll.veds.mail.domain.error

import com.vertyll.veds.sharederror.DomainError
import com.vertyll.veds.sharederror.ErrorKind

enum class MailError(
    override val key: String,
    override val kind: ErrorKind,
) : DomainError {
    TEMPLATE_UNKNOWN("mail.template.unknown", ErrorKind.INVALID),
}
