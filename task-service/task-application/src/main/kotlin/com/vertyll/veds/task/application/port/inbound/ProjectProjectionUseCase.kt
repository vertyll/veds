package com.vertyll.veds.task.application.port.inbound

import com.vertyll.veds.task.domain.model.ProjectCategoryRef
import com.vertyll.veds.task.domain.model.ProjectMembershipRef
import com.vertyll.veds.task.domain.model.ProjectRef
import com.vertyll.veds.task.domain.model.ProjectStatusRef
import java.util.UUID

interface ProjectProjectionUseCase {
    fun projectChanged(project: ProjectRef)

    fun projectArchived(projectId: UUID)

    fun categoryChanged(category: ProjectCategoryRef)

    fun categoryRemoved(categoryId: UUID)

    fun statusChanged(status: ProjectStatusRef)

    fun statusRemoved(statusId: UUID)

    fun memberJoined(membership: ProjectMembershipRef)

    fun memberRemoved(
        projectId: UUID,
        userId: UUID,
    )
}
