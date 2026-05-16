package com.vertyll.veds.iam.infrastructure.saga

import com.vertyll.veds.iam.application.service.AuthCompensationService
import com.vertyll.veds.sharedinfrastructure.saga.service.SagaCompensationHandler

class IamSagaCompensationHandler(
    private val authCompensationService: AuthCompensationService,
) : SagaCompensationHandler {
    override fun handle(
        sagaId: String,
        action: String,
        event: Map<String, Any?>,
    ) = authCompensationService.compensate(action, event)
}
