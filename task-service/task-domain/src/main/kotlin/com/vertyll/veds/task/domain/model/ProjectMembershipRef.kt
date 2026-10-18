package com.vertyll.veds.task.domain.model

import java.time.Instant
import java.util.UUID

data class ProjectMembershipRef(
    val projectId: UUID,
    val userId: UUID,
    val roleCode: String,
    val updatedAt: Instant = Instant.now(),
)
