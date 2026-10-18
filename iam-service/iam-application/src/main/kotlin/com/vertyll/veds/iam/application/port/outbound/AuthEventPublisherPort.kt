package com.vertyll.veds.iam.application.port.outbound

import java.util.UUID

@Suppress("kotlin:S6517")
interface AuthEventPublisherPort {
    fun requestMail(
        to: String,
        templateName: String,
        variables: Map<String, String>,
        replyTo: String? = null,
        priority: Int = 0,
        sagaId: String? = null,
    )

    fun publishUserRegistered(
        userId: UUID,
        email: String,
        firstName: String?,
        lastName: String?,
    )

    fun publishUserProfileUpdated(
        userId: UUID,
        email: String,
        firstName: String?,
        lastName: String?,
        avatarFileId: UUID?,
    )
}
