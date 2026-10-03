package com.vertyll.veds.project.infrastructure.saga

import com.vertyll.veds.project.application.saga.model.ProjectCompensationCommand
import com.vertyll.veds.project.application.saga.model.SagaStepNames
import com.vertyll.veds.project.infrastructure.persistence.entity.SagaJpaEntity
import com.vertyll.veds.project.infrastructure.persistence.entity.SagaStepJpaEntity
import com.vertyll.veds.shared.saga.engine.SagaCompensationContext
import com.vertyll.veds.shared.saga.engine.SagaCompensator
import org.slf4j.LoggerFactory

internal class ProjectSagaCompensator : SagaCompensator<SagaJpaEntity, SagaStepJpaEntity, ProjectCompensationCommand> {
    private val logger = LoggerFactory.getLogger(ProjectSagaCompensator::class.java)

    private companion object {
        private const val INVITATION_ID_KEY = "invitationId"
        private const val PROJECT_ID_KEY = "projectId"
        private const val MAIL_FAILED_REASON = "Invitation e-mail could not be delivered"
        private const val ARCHIVAL_FAILED_REASON = "Downstream cleanup after project archival failed"
    }

    override fun compensateStep(
        saga: SagaJpaEntity,
        step: SagaStepJpaEntity,
        context: SagaCompensationContext<ProjectCompensationCommand>,
    ) {
        val command =
            when (step.stepName) {
                SagaStepNames.PERSIST_INVITATION.value -> {
                    ProjectCompensationCommand.RevokeInvitation(
                        invitationId = readKey(context, step, INVITATION_ID_KEY),
                        reason = MAIL_FAILED_REASON,
                    )
                }

                SagaStepNames.ARCHIVE_PROJECT.value -> {
                    ProjectCompensationCommand.RestoreProject(
                        projectId = readKey(context, step, PROJECT_ID_KEY),
                        reason = ARCHIVAL_FAILED_REASON,
                    )
                }

                SagaStepNames.REQUEST_INVITATION_MAIL.value,
                SagaStepNames.PUBLISH_PROJECT_ARCHIVED.value,
                -> {
                    logger.info(
                        "No compensation needed for step '{}' on saga '{}' (effect not externally reversible)",
                        step.stepName,
                        saga.id,
                    )
                    return
                }

                else -> {
                    logger.warn("No compensation defined for step '{}' on saga '{}'", step.stepName, saga.id)
                    return
                }
            }

        context.publishCompensationEvent(
            sagaId = saga.id,
            stepId = step.id,
            command = command,
        )
    }

    private fun readKey(
        context: SagaCompensationContext<ProjectCompensationCommand>,
        step: SagaStepJpaEntity,
        key: String,
    ): String {
        val payload = context.readStepPayload(step.payload)
        val raw =
            payload[key]
                ?: error("Missing '$key' in step payload for step ${step.id} (keys: ${payload.keys})")
        return raw.toString()
    }
}
