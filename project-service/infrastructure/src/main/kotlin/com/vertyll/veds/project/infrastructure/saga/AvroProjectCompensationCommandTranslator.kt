package com.vertyll.veds.project.infrastructure.saga

import com.vertyll.veds.project.application.saga.model.ProjectCompensationCommand
import com.vertyll.veds.project.saga.RestoreProjectAction
import com.vertyll.veds.project.saga.RevokeInvitationAction
import com.vertyll.veds.project.saga.SagaCompensationEvent
import com.vertyll.veds.shared.messaging.avro.AvroPayloadDeserializer
import com.vertyll.veds.shared.saga.engine.CompensationCommandDeserializer
import com.vertyll.veds.shared.saga.engine.DecodedCompensationEvent

internal class AvroProjectCompensationCommandTranslator(
    private val avroPayloadDeserializer: AvroPayloadDeserializer,
    private val topic: String,
) : CompensationCommandDeserializer<ProjectCompensationCommand> {
    override fun deserialize(payload: ByteArray): DecodedCompensationEvent<ProjectCompensationCommand> {
        val record = avroPayloadDeserializer.deserialize(topic, payload) as SagaCompensationEvent
        val command =
            when (val action = record.action) {
                is RevokeInvitationAction -> {
                    ProjectCompensationCommand.RevokeInvitation(
                        invitationId = action.invitationId.toString(),
                        reason = action.reason.toString(),
                    )
                }

                is RestoreProjectAction -> {
                    ProjectCompensationCommand.RestoreProject(
                        projectId = action.projectId.toString(),
                        reason = action.reason.toString(),
                    )
                }

                else -> {
                    error(
                        "Unknown compensation action type on saga-compensation-project: ${action?.javaClass?.name}",
                    )
                }
            }
        return DecodedCompensationEvent(
            sagaId = record.sagaId.toString(),
            stepId = record.stepId,
            command = command,
        )
    }
}
