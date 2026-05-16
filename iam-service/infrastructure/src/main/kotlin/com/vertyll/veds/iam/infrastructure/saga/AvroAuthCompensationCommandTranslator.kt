package com.vertyll.veds.iam.infrastructure.saga

import com.vertyll.veds.iam.application.saga.model.AuthCompensationCommand
import com.vertyll.veds.iam.saga.DeleteUserAction
import com.vertyll.veds.iam.saga.DeleteVerificationTokenAction
import com.vertyll.veds.iam.saga.RevertEmailUpdateAction
import com.vertyll.veds.iam.saga.RevertPasswordUpdateAction
import com.vertyll.veds.iam.saga.SagaCompensationEvent
import com.vertyll.veds.sharedinfrastructure.avro.AvroPayloadDeserializer
import com.vertyll.veds.sharedinfrastructure.saga.service.CompensationCommandDeserializer
import com.vertyll.veds.sharedinfrastructure.saga.service.DecodedCompensationEvent

/**
 * Anti-Corruption Layer (DDD) translating raw Avro bytes received on the
 * `saga-compensation-iam` topic into the application-layer sealed
 * [AuthCompensationCommand] hierarchy.
 *
 * This is the **only** place where Avro generated types meet the IAM
 * domain — the application layer therefore stays free of Avro, Jackson,
 * Kafka and stringly-typed dispatch.
 *
 * Mirrors the tagged union declared in
 * `contracts/iam-service/saga-compensation-iam/v1/saga-compensation.avsc`.
 * Each branch is exhaustive — adding a new compensation action without
 * updating the translator becomes a compile-time error.
 */
internal class AvroAuthCompensationCommandTranslator(
    private val avroPayloadDeserializer: AvroPayloadDeserializer,
    private val topic: String,
) : CompensationCommandDeserializer<AuthCompensationCommand> {
    override fun deserialize(payload: ByteArray): DecodedCompensationEvent<AuthCompensationCommand> {
        val record = avroPayloadDeserializer.deserialize(topic, payload) as SagaCompensationEvent
        val command =
            when (val action = record.action) {
                is DeleteUserAction ->
                    AuthCompensationCommand.DeleteUser(userId = action.userId)
                is DeleteVerificationTokenAction ->
                    AuthCompensationCommand.DeleteVerificationToken(tokenId = action.tokenId)
                is RevertPasswordUpdateAction ->
                    AuthCompensationCommand.RevertPasswordUpdate(userId = action.userId)
                is RevertEmailUpdateAction ->
                    AuthCompensationCommand.RevertEmailUpdate(
                        userId = action.userId,
                        originalEmail = action.originalEmail.toString(),
                    )
                else ->
                    error(
                        "Unknown compensation action type on saga-compensation-iam: ${action?.javaClass?.name}",
                    )
            }
        return DecodedCompensationEvent(
            sagaId = record.sagaId.toString(),
            stepId = record.stepId,
            command = command,
        )
    }
}
