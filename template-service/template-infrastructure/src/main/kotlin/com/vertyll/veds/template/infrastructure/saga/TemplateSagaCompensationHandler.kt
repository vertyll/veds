package com.vertyll.veds.template.infrastructure.saga

import com.vertyll.veds.shared.saga.engine.CompensationCommandHandler
import com.vertyll.veds.template.application.port.inbound.TemplateCompensationUseCase
import com.vertyll.veds.template.application.saga.model.TemplateCompensationCommand

internal class TemplateSagaCompensationHandler(
    private val templateCompensationService: TemplateCompensationUseCase,
) : CompensationCommandHandler<TemplateCompensationCommand> {
    override fun handle(
        sagaId: String,
        command: TemplateCompensationCommand,
    ) = templateCompensationService.compensate(command)
}
