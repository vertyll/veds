package com.vertyll.veds.notification.application

import com.vertyll.veds.notification.application.port.outbound.MailRequestPort
import com.vertyll.veds.notification.domain.model.NotificationType

internal class RecordingMailRequests : MailRequestPort {
    val requested = mutableListOf<String>()
    val sagaIds = mutableListOf<String?>()

    override fun requestMail(
        to: String,
        type: NotificationType,
        params: Map<String, String>,
        originSagaId: String?,
    ) {
        requested += "$to:${type.name}"
        sagaIds += originSagaId
    }
}
