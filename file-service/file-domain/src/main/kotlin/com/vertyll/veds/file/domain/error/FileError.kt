package com.vertyll.veds.file.domain.error

import com.vertyll.veds.sharederror.DomainError
import com.vertyll.veds.sharederror.ErrorKind

enum class FileError(
    override val key: String,
    override val kind: ErrorKind,
) : DomainError {
    FILE_NOT_FOUND("file.not_found", ErrorKind.NOT_FOUND),
    FILE_NOT_AVAILABLE("file.not_available", ErrorKind.CONFLICT),

    CONTENT_TYPE_NOT_ALLOWED("file.content_type_not_allowed", ErrorKind.INVALID),
    FILE_TOO_LARGE("file.too_large", ErrorKind.INVALID),
    UPLOAD_NOT_PENDING("file.upload_not_pending", ErrorKind.CONFLICT),
    OBJECT_MISSING_IN_STORAGE("file.object_missing_in_storage", ErrorKind.CONFLICT),
    TOKEN_CLAIM_MISSING("common.token_claim_missing", ErrorKind.ACCESS_DENIED),
}
