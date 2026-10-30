package com.vertyll.veds.task.infrastructure.persistence.entity

import java.io.Serializable
import java.util.UUID

internal data class ProjectMembershipRefId(
    val projectId: UUID? = null,
    val userId: UUID? = null,
) : Serializable {
    companion object {
        private const val serialVersionUID: Long = 1L
    }
}
