package com.vertyll.veds.project.application.saga.model

sealed interface ProjectCompensationCommand {
    data class RevokeInvitation(
        val invitationId: String,
        val reason: String,
    ) : ProjectCompensationCommand

    data class RestoreProject(
        val projectId: String,
        val reason: String,
    ) : ProjectCompensationCommand
}
