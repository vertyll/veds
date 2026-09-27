package com.vertyll.veds.iam.application

import com.vertyll.veds.iam.application.port.outbound.AuthEventPublisherPort
import java.util.UUID

internal class RecordingAuthEventPublisher : AuthEventPublisherPort {
    val published = mutableListOf<String>()

    override fun requestMail(
        to: String,
        templateName: String,
        variables: Map<String, String>,
        replyTo: String?,
        priority: Int,
        sagaId: String?,
    ) {
        published += "MailRequested($to,$templateName)"
    }

    override fun publishUserRegistered(
        userId: UUID,
        email: String,
        firstName: String?,
        lastName: String?,
    ) {
        published += "UserRegistered($email)"
    }

    override fun publishUserProfileUpdated(
        userId: UUID,
        email: String,
        firstName: String?,
        lastName: String?,
        avatarFileId: UUID?,
    ) {
        published += "UserProfileUpdated($email)"
    }
}
