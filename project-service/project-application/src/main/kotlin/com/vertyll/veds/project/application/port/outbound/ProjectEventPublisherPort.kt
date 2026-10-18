package com.vertyll.veds.project.application.port.outbound

import java.util.UUID

@Suppress("TooManyFunctions")
interface ProjectEventPublisherPort {
    fun publishProjectCreated(
        projectId: UUID,
        name: String,
        ownerId: UUID,
        hiddenWorkLogEnabled: Boolean,
    )

    fun publishProjectUpdated(
        projectId: UUID,
        name: String,
        hiddenWorkLogEnabled: Boolean,
    )

    fun publishProjectArchived(
        projectId: UUID,
        sagaId: String? = null,
    )

    fun publishMemberInvited(
        projectId: UUID,
        projectName: String,
        invitationId: UUID,
        inviteeEmail: String,
        inviterId: UUID,
        sagaId: String? = null,
    )

    fun publishMemberJoined(
        projectId: UUID,
        memberId: UUID,
        userId: UUID,
        roleCode: String,
    )

    fun publishMemberRemoved(
        projectId: UUID,
        userId: UUID,
    )

    fun publishCategoryChanged(
        projectId: UUID,
        categoryId: UUID,
        names: Map<String, String>,
        color: String,
        removed: Boolean,
    )

    fun publishStatusChanged(
        projectId: UUID,
        statusId: UUID,
        names: Map<String, String>,
        color: String,
        removed: Boolean,
    )
}
