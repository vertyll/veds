package com.vertyll.veds.iam.application.port.out

@Suppress("kotlin:S6517")
interface AuthEventPublisherPort {
    fun sendMailRequestedEvent(
        to: String,
        subject: String,
        templateName: String,
        variables: Map<String, String>,
        replyTo: String? = null,
        priority: Int = 0,
        sagaId: String? = null,
    )
}
