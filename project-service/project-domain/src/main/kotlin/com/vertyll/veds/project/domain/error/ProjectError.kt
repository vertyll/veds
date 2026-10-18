package com.vertyll.veds.project.domain.error

import com.vertyll.veds.sharederror.DomainError
import com.vertyll.veds.sharederror.ErrorKind

enum class ProjectError(
    override val key: String,
    override val kind: ErrorKind,
) : DomainError {
    PROJECT_NOT_FOUND("project.not_found", ErrorKind.NOT_FOUND),
    PROJECT_ACCESS_DENIED("project.access_denied", ErrorKind.ACCESS_DENIED),
    PROJECT_ARCHIVED("project.archived", ErrorKind.CONFLICT),

    TYPE_NOT_FOUND("project.type.not_found", ErrorKind.NOT_FOUND),

    ROLE_NOT_FOUND("project.role.not_found", ErrorKind.NOT_FOUND),
    ROLE_NOT_CONFIGURED("project.role.not_configured", ErrorKind.MISCONFIGURED),

    CATEGORY_NOT_FOUND("project.category.not_found", ErrorKind.NOT_FOUND),
    STATUS_NOT_FOUND("project.status.not_found", ErrorKind.NOT_FOUND),

    MEMBER_NOT_FOUND("project.member.not_found", ErrorKind.NOT_FOUND),
    MEMBER_ALREADY_JOINED("project.member.already_joined", ErrorKind.CONFLICT),
    MEMBER_OWNER_IMMUTABLE("project.member.owner_immutable", ErrorKind.CONFLICT),

    INVITATION_NOT_FOUND("project.invitation.not_found", ErrorKind.NOT_FOUND),
    INVITATION_NOT_PENDING("project.invitation.not_pending", ErrorKind.CONFLICT),
    INVITATION_EXPIRED("project.invitation.expired", ErrorKind.GONE),
    INVITATION_ALREADY_SENT("project.invitation.already_sent", ErrorKind.CONFLICT),
    INVITATION_NOT_ADDRESSED_TO_CALLER("project.invitation.not_addressed_to_caller", ErrorKind.ACCESS_DENIED),

    TRANSLATION_MISSING("project.translation.missing", ErrorKind.INVALID),

    VERSION_MISMATCH("common.version_mismatch", ErrorKind.PRECONDITION_FAILED),
    LANGUAGE_NOT_SUPPLIED("common.language_not_supplied", ErrorKind.INVALID),
    LANGUAGE_NOT_SUPPORTED("common.language_not_supported", ErrorKind.INVALID),
    TOKEN_CLAIM_MISSING("common.token_claim_missing", ErrorKind.ACCESS_DENIED),
}
