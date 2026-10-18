package com.vertyll.veds.project.application

import com.vertyll.veds.project.application.port.outbound.ProjectEventPublisherPort
import java.util.UUID

internal open class RecordingEventPublisher : ProjectEventPublisherPort {
    val published = mutableListOf<String>()

    override fun publishProjectCreated(
        projectId: UUID,
        name: String,
        ownerId: UUID,
        hiddenWorkLogEnabled: Boolean,
    ) {
        published += "ProjectCreated($projectId)"
    }

    override fun publishProjectUpdated(
        projectId: UUID,
        name: String,
        hiddenWorkLogEnabled: Boolean,
    ) {
        published += "ProjectUpdated($projectId)"
    }

    override fun publishProjectArchived(
        projectId: UUID,
        sagaId: String?,
    ) {
        published += "ProjectArchived($projectId)"
    }

    override fun publishMemberInvited(
        projectId: UUID,
        projectName: String,
        invitationId: UUID,
        inviteeEmail: String,
        inviterId: UUID,
        sagaId: String?,
    ) {
        published += "MemberInvited($projectId,$inviteeEmail)"
    }

    override fun publishMemberJoined(
        projectId: UUID,
        memberId: UUID,
        userId: UUID,
        roleCode: String,
    ) {
        published += "MemberJoined($projectId,$userId,$roleCode)"
    }

    override fun publishMemberRemoved(
        projectId: UUID,
        userId: UUID,
    ) {
        published += "MemberRemoved($projectId,$userId)"
    }

    override fun publishCategoryChanged(
        projectId: UUID,
        categoryId: UUID,
        names: Map<String, String>,
        color: String,
        removed: Boolean,
    ) {
        published += "CategoryChanged($projectId,$categoryId,removed=$removed)"
    }

    override fun publishStatusChanged(
        projectId: UUID,
        statusId: UUID,
        names: Map<String, String>,
        color: String,
        removed: Boolean,
    ) {
        published += "StatusChanged($projectId,$statusId,removed=$removed)"
    }
}
