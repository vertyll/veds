package com.vertyll.veds.iam.infrastructure.saga

import com.vertyll.veds.iam.application.port.inbound.AuthCompensationUseCase
import com.vertyll.veds.iam.application.saga.model.AuthCompensationCommand
import com.vertyll.veds.sharedinfrastructure.saga.service.CompensationCommandHandler

/**
 * Infrastructure-layer adapter wiring the shared
 * [CompensationCommandHandler] port to the application-layer
 * [AuthCompensationUseCase]. Keeps the application port Kafka/Avro-free
 * while letting the shared engine remain a pure technical building block.
 */
internal class IamSagaCompensationHandler(
    private val authCompensationService: AuthCompensationUseCase,
) : CompensationCommandHandler<AuthCompensationCommand> {
    override fun handle(
        sagaId: String,
        command: AuthCompensationCommand,
    ) = authCompensationService.compensate(command)
}
