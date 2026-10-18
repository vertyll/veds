package com.vertyll.veds.project.application.dto

import com.vertyll.veds.project.domain.model.Project
import java.time.Instant
import java.util.UUID

data class ProjectResponse(
    val id: UUID,
    val name: String,
    val description: String?,
    val isPublic: Boolean,
    val isActive: Boolean,
    val typeId: UUID?,
    val iconFileId: UUID?,
    val ownerId: UUID,
    val hiddenWorkLogEnabled: Boolean,
    val createdAt: Instant,
    val updatedAt: Instant,
    val version: Long?,
    val permissions: Set<String> = emptySet(),
) {
    companion object {
        fun from(
            project: Project,
            permissions: Set<String> = emptySet(),
        ): ProjectResponse =
            ProjectResponse(
                id = project.id,
                name = project.name,
                description = project.description,
                isPublic = project.isPublic,
                isActive = project.isActive,
                typeId = project.typeId,
                iconFileId = project.iconFileId,
                ownerId = project.ownerId,
                hiddenWorkLogEnabled = project.hiddenWorkLogEnabled,
                createdAt = project.createdAt,
                updatedAt = project.updatedAt,
                version = project.version,
                permissions = permissions,
            )
    }
}
