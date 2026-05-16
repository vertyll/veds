package com.vertyll.veds.template.application.port.inbound

import com.vertyll.veds.template.application.saga.model.TemplateCompensationCommand

/**
 * Application-layer inbound port for executing compensation actions on
 * the template bounded context.
 *
 * Mirrors `AuthCompensationUseCase` in iam-service. The infrastructure
 * Kafka listener (`SagaCompensationService`) ultimately delegates here
 * after the Avro wire format has been decoded into a typed
 * [TemplateCompensationCommand] by `AvroTemplateCompensationCommandTranslator`.
 *
 * Implementations should dispatch via an exhaustive `when` over the
 * sealed hierarchy — adding a new compensation variant will then become
 * a compile-time error here instead of a runtime failure.
 */
fun interface TemplateCompensationUseCase {
    fun compensate(command: TemplateCompensationCommand)
}
