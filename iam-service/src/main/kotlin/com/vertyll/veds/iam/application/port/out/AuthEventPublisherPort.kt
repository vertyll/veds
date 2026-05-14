package com.vertyll.veds.iam.application.port.out

import com.vertyll.veds.sharedinfrastructure.event.mail.MailRequestedEvent

interface AuthEventPublisherPort {
    fun sendMailRequestedEvent(event: MailRequestedEvent)
}
