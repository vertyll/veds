package com.vertyll.veds.iam.application.saga.model

import com.vertyll.veds.iam.application.port.inbound.AuthCompensationUseCase

/**
 * Application-layer compensation commands for the IAM bounded context.
 *
 * A `sealed interface` mirroring the Avro tagged union sitting on the
 * `saga-compensation-iam` topic
 * (`contracts/iam-service/saga-compensation-iam/v1/saga-compensation.avsc`).
 *
 * Lives in the application layer so the layer can stay framework-free
 * (no Avro, no Spring, no Kafka, no Jackson). The translation between the
 * Avro wire format and this type lives in the infrastructure layer as an
 * Anti-Corruption Layer (DDD) — see `AvroAuthCompensationCommandTranslator`.
 *
 * Each subtype carries the strongly-typed data needed by
 * [AuthCompensationUseCase],
 * which dispatches via an exhaustive `when` — adding a new compensation
 * action without updating every consumer becomes a compile error rather
 * than a runtime NPE.
 */
sealed interface AuthCompensationCommand {
    /**
     * Compensates the `CreateUser` saga step — deletes the local user
     * aggregate that the failed registration created.
     */
    data class DeleteUser(
        val userId: Long,
    ) : AuthCompensationCommand

    /**
     * Compensates the `CreateVerificationToken` saga step — deletes the
     * locally stored verification token (e.g. for activation, password
     * reset, email change).
     */
    data class DeleteVerificationToken(
        val tokenId: Long,
    ) : AuthCompensationCommand

    /**
     * Compensates the `UpdatePassword` saga step.
     *
     * Password material never crosses the wire — the service performs
     * whatever rollback is possible (today: just logging, because
     * Keycloak owns the password) using only [userId].
     */
    data class RevertPasswordUpdate(
        val userId: Long,
    ) : AuthCompensationCommand

    /**
     * Compensates the `UpdateEmail` saga step — reverts the user's email
     * back to [originalEmail] in both the local store and Keycloak.
     */
    data class RevertEmailUpdate(
        val userId: Long,
        val originalEmail: String,
    ) : AuthCompensationCommand
}
