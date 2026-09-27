package com.vertyll.veds.task.domain.model

import java.time.Instant
import java.util.UUID

data class ProjectRef(
    val projectId: UUID,
    val name: String,
    val isActive: Boolean = true,
    val hiddenWorkLogEnabled: Boolean = false,
    val updatedAt: Instant = Instant.now(),
)
