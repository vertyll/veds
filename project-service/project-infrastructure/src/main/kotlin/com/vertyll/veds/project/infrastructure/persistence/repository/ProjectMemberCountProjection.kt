package com.vertyll.veds.project.infrastructure.persistence.repository

import java.util.UUID

internal interface ProjectMemberCountProjection {
    val projectId: UUID
    val memberCount: Long
}
