package com.vertyll.veds.task.infrastructure.persistence.entity

import java.io.Serializable
import java.util.UUID

internal data class ProjectMembershipRefId(
    var projectId: UUID? = null,
    var userId: UUID? = null,
) : Serializable {
    companion object {
        private const val serialVersionUID: Long = 1L
    }
}
