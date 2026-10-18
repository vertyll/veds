package com.vertyll.veds.project.application.saga.model

import com.vertyll.veds.shared.saga.SagaTypeValue

enum class SagaStepNames(
    override val value: String,
) : SagaTypeValue {
    PERSIST_INVITATION("PersistInvitation"),
    REQUEST_INVITATION_MAIL("RequestInvitationMail"),
    ARCHIVE_PROJECT("ArchiveProject"),
    PUBLISH_PROJECT_ARCHIVED("PublishProjectArchived"),
}
