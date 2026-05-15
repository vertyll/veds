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
interface SagaCompensationHandler {
    fun handle(
        sagaId: String,
        action: String,
        event: Map<String, Any?>,
    )
}
