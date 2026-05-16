package com.vertyll.veds.iam.infrastructure.saga

import com.vertyll.veds.iam.application.port.inbound.AuthCompensationUseCase
import com.vertyll.veds.sharedinfrastructure.saga.service.SagaCompensationHandler

internal class IamSagaCompensationHandler(
    private val authCompensationService: AuthCompensationUseCase,
) : SagaCompensationHandler {
    override fun handle(
        sagaId: String,
        action: String,
        event: Map<String, Any?>,
    ) = authCompensationService.compensate(action, event)
}
