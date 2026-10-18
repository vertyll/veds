package com.vertyll.veds.template.application.port.inbound

import com.vertyll.veds.template.application.saga.model.TemplateCompensationCommand

fun interface TemplateCompensationUseCase {
    fun compensate(command: TemplateCompensationCommand)
}
