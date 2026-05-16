package com.vertyll.veds.sharedinfrastructure.saga.service

/**
 * Non-generic trigger used by [SagaEngine] to delegate compensation to a
 * proxied bean that opens a fresh `REQUIRES_NEW` transaction.
 *
 * Extracted so [SagaEngine] does not need to know the command type
 * parameter of the underlying [SagaCompensationRunner], keeping the
 * engine's signature stable for all participants.
 */
@Suppress("kotlin:S6517")
fun interface SagaCompensationTrigger {
    /**
     * Runs the compensation pipeline for [sagaId]. Idempotent — may be
     * called multiple times (e.g. by [SagaWatchdog]) without ill effects.
     */
    fun runCompensation(sagaId: String)
}
