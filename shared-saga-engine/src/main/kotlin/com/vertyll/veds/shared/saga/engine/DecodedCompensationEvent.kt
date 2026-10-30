package com.vertyll.veds.shared.saga.engine

/**
 * Strongly-typed envelope produced by [CompensationCommandDeserializer].
 *
 * @param TCommand the service's own compensation command.
 * @property sagaId  originating saga's id (Saga Log Correlation).
 * @property stepId  optional id of the step being compensated; `null` for saga-level actions.
 * @property command service-local typed compensation command (sealed hierarchy).
 */
data class DecodedCompensationEvent<TCommand : Any>(
    val sagaId: String,
    val stepId: Long?,
    val command: TCommand,
)
