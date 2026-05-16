package com.vertyll.veds.template.infrastructure.saga

import com.vertyll.veds.sharedinfrastructure.saga.service.CompensationCommandHandler
import com.vertyll.veds.template.application.port.inbound.TemplateCompensationUseCase
import com.vertyll.veds.template.application.saga.model.TemplateCompensationCommand

/**
 * Infrastructure-layer adapter wiring the shared
 * [CompensationCommandHandler] port to the application-layer
 * [TemplateCompensationUseCase]. Keeps the application port
 * Kafka/Avro-free while letting the shared engine remain a pure
 * technical building block.
 */
internal class TemplateSagaCompensationHandler(
    private val templateCompensationService: TemplateCompensationUseCase,
) : CompensationCommandHandler<TemplateCompensationCommand> {
    override fun handle(
        sagaId: String,
        command: TemplateCompensationCommand,
    ) = templateCompensationService.compensate(command)
}
