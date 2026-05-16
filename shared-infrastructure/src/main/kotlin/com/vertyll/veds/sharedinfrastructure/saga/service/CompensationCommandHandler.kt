package com.vertyll.veds.sharedinfrastructure.saga.service

/**
 * Domain-specific compensation dispatcher used by [SagaCompensationEngine].
 *
 * Implementations dispatch on the typed [TCommand] (typically a Kotlin
 * `sealed interface` mirroring the Avro tagged union) and invoke
 * domain-level compensations (e.g. delete user, revert email update) —
 * usually by delegating to an application-layer use case.
 *
 * Strongly-typed by design — replaces the previous untyped
 * `Map<String, Any?>` envelope and `action: String` discriminator with an
 * exhaustive `when` on the sealed hierarchy.
 */
@Suppress("kotlin:S6517")
fun interface CompensationCommandHandler<TCommand : Any> {
    /**
     * Performs the domain-specific compensation for a single event.
     *
     * @param sagaId  saga correlation id from the inbound event envelope.
     * @param command typed compensation command — exhaustive over the
     *   service's compensation actions.
     *
     * Implementations should NOT swallow exceptions; let them propagate
     * so the inbound Kafka listener can trigger broker-level retry / DLT.
     */
    fun handle(
        sagaId: String,
        command: TCommand,
    )
}
