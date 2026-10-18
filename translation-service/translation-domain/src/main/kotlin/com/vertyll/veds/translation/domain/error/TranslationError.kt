package com.vertyll.veds.translation.domain.error

import com.vertyll.veds.sharederror.DomainError
import com.vertyll.veds.sharederror.ErrorKind

enum class TranslationError(
    override val key: String,
    override val kind: ErrorKind,
) : DomainError {
    KEY_NOT_FOUND("translation.key.not_found", ErrorKind.NOT_FOUND),
    KEY_OWNED_BY_ANOTHER_SERVICE("translation.key.owned_by_another_service", ErrorKind.CONFLICT),

    LANGUAGE_NOT_FOUND("translation.language.not_found", ErrorKind.NOT_FOUND),
    LANGUAGE_NOT_SUPPLIED("common.language_not_supplied", ErrorKind.INVALID),
    LANGUAGE_NOT_SUPPORTED("common.language_not_supported", ErrorKind.INVALID),

    VALUE_NOT_FOUND("translation.value.not_found", ErrorKind.NOT_FOUND),
    INVALID_ICU_PATTERN("translation.value.invalid_icu_pattern", ErrorKind.INVALID),
    UNKNOWN_ARGUMENTS("translation.value.unknown_arguments", ErrorKind.INVALID),

    IMPORT_MALFORMED("translation.import.malformed", ErrorKind.INVALID),

    VERSION_MISMATCH("common.version_mismatch", ErrorKind.PRECONDITION_FAILED),
    TOKEN_CLAIM_MISSING("common.token_claim_missing", ErrorKind.ACCESS_DENIED),
}
