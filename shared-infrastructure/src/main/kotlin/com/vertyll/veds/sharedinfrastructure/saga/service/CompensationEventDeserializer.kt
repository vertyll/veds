package com.vertyll.veds.sharedinfrastructure.saga.service

/**
 * Service-provided deserializer for compensation events consumed from the
 * internal `saga-compensation-<participant>` topic.
 *
 * Counterpart of [CompensationEventSerializer]; the wire format is up to
 * the owning service. The returned map MUST contain at least the keys
 * `sagaId` (`String`), `action` (`String`), and optionally `stepId`
 * (`Number`) — `SagaCompensationEngine` reads these to dispatch the
 * compensation action.
 */
@Suppress("kotlin:S6517")
interface CompensationEventDeserializer {
    /**
     * Decodes a raw compensation-event [payload] into a property map. The
     * returned map MUST contain `sagaId: String` and `action: String`, and
     * MAY contain `stepId: Number`.
     */
    fun deserializeCompensationEvent(payload: ByteArray): Map<String, Any?>
}
