package com.vertyll.veds.template.application.saga.model

import com.vertyll.veds.template.application.port.inbound.TemplateCompensationUseCase

/**
 * Application-layer compensation commands for the template bounded context.
 *
 * Sealed mirror of the Avro tagged union sitting on the
 * `saga-compensation-template` topic
 * (`template-contracts/avro/saga-compensation-template/v1/saga-compensation.avsc`).
 *
 * Lives in the application layer so the layer can stay framework-free
 * (no Avro, no Spring, no Kafka, no Jackson). The translation between
 * the Avro wire format and this type lives in the infrastructure layer
 * as an Anti-Corruption Layer (DDD) — see
 * `AvroTemplateCompensationCommandTranslator`.
 *
 * Each subtype carries the strongly-typed data needed by
 * [TemplateCompensationUseCase], which dispatches via an exhaustive
 * `when` — adding a new compensation action without updating every
 * consumer becomes a compile error rather than a runtime NPE.
 *
 * This is a placeholder hierarchy meant to be renamed / extended when
 * cloning template-service into a real bounded context.
 */
sealed interface TemplateCompensationCommand {
    /**
     * Compensates the `PersistTemplate` saga step — deletes the local
     * template aggregate that the failed flow created.
     */
    data class DeleteTemplate(
        val templateId: String,
    ) : TemplateCompensationCommand

    /**
     * Compensates the `PublishTemplateEvent` saga step.
     *
     * The published Kafka event cannot be un-published, so the
     * compensation only records a log entry. Kept as a placeholder for
     * clone-time replacement (e.g. with a "publish reversal" event).
     */
    data class LogTemplateCompensation(
        val templateId: String,
    ) : TemplateCompensationCommand
}
