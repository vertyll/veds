package com.vertyll.veds.sharedinfrastructure.saga.service

@Suppress("kotlin:S6517")
interface CompensationEventDeserializer {
    fun deserializeCompensationEvent(payload: ByteArray): Map<String, Any?>
}
