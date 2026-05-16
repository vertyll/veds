package com.vertyll.veds.iam.application.port.inbound

import com.vertyll.veds.iam.application.saga.model.AuthCompensationCommand

/**
 * Inbound port for the IAM compensation use case.
 *
 * Accepts a strongly-typed [AuthCompensationCommand] — the application
 * layer therefore stays free of Avro, Jackson, Kafka, and untyped maps.
 * Translation between the Avro wire format and this type is performed
 * by the infrastructure-layer Anti-Corruption Layer (translator).
 */
@Suppress("kotlin:S6517")
fun interface AuthCompensationUseCase {
    /**
     * Performs the compensation described by [command]. Implementations
     * MUST be idempotent — the watchdog and Kafka broker may redeliver
     * the same compensation event.
     *
     * Implementations should NOT swallow exceptions — propagate so the
     * inbound Kafka listener can trigger broker-level retry / DLT.
     */
    fun compensate(command: AuthCompensationCommand)
}
