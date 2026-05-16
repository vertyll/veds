package com.vertyll.veds.sharedinfrastructure.saga.service

@Suppress("kotlin:S6517")
interface CompensationEventSerializer {
    fun serializeCompensationEvent(
        sagaId: String,
        stepId: Long?,
        action: String,
        extraPayload: Map<String, Any?>,
    ): ByteArray
}
