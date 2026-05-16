package com.vertyll.veds.sharedinfrastructure.saga.service

/**
 * Domain-specific compensation dispatcher used by [SagaCompensationEngine].
 *
 * Implementations should dispatch on the `action` field and invoke
 * domain-level compensations (e.g. delete user, revert email update).
 *
 * Replaces the abstract `processCompensation` method previously found on
 * `BaseSagaCompensationService` (Template Method) with composition.
 */
@Suppress("kotlin:S6517")
interface SagaCompensationHandler {
    /**
     * Performs the domain-specific compensation for a single event.
     *
     * @param sagaId saga correlation id from the inbound event envelope.
     * @param action compensation action discriminator (e.g. `"DELETE_USER"`).
     * @param event raw event payload as a property map for dispatch-time
     *   destructuring.
     */
    fun handle(
        sagaId: String,
        action: String,
        event: Map<String, Any?>,
    )
}
