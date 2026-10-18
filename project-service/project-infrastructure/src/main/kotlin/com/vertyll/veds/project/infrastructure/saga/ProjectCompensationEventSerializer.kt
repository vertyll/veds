package com.vertyll.veds.project.infrastructure.saga

import com.vertyll.veds.project.application.saga.model.ProjectCompensationCommand
import com.vertyll.veds.project.saga.RestoreProjectAction
import com.vertyll.veds.project.saga.RevokeInvitationAction
import com.vertyll.veds.project.saga.SagaCompensationEvent
import com.vertyll.veds.shared.messaging.avro.AvroPayloadSerializer
import com.vertyll.veds.shared.saga.engine.CompensationEventSerializer

internal class ProjectCompensationEventSerializer(
    private val avroPayloadSerializer: AvroPayloadSerializer,
    private val topic: String,
) : CompensationEventSerializer<ProjectCompensationCommand> {
    override fun serialize(
        sagaId: String,
        stepId: Long?,
        command: ProjectCompensationCommand,
    ): ByteArray {
        val action: Any =
            when (command) {
                is ProjectCompensationCommand.RevokeInvitation -> {
                    RevokeInvitationAction
                        .newBuilder()
                        .setInvitationId(command.invitationId)
                        .setReason(command.reason)
                        .build()
                }

                is ProjectCompensationCommand.RestoreProject -> {
                    RestoreProjectAction
                        .newBuilder()
                        .setProjectId(command.projectId)
                        .setReason(command.reason)
                        .build()
                }
            }
        val record =
            SagaCompensationEvent
                .newBuilder()
                .setSagaId(sagaId)
                .setStepId(stepId)
                .setAction(action)
                .build()
        return avroPayloadSerializer.serialize(topic, record)
    }
}
