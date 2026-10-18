package com.vertyll.veds.template.domain.error

import com.vertyll.veds.sharederror.DomainError
import com.vertyll.veds.sharederror.ErrorKind

enum class TemplateError(
    override val key: String,
    override val kind: ErrorKind,
) : DomainError {
    NOT_FOUND("template.not_found", ErrorKind.NOT_FOUND),
}
