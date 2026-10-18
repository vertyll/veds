package com.vertyll.veds.project.application.dto

import com.vertyll.veds.project.domain.model.Project
import java.time.Instant
import java.util.UUID

data class ProjectListItemResponse(
    val id: UUID,
    val name: String,
    val description: String?,
    val isPublic: Boolean,
    val isActive: Boolean,
    val iconFileId: UUID?,
    val typeId: UUID?,
    val memberCount: Int,
    val createdAt: Instant,
    val version: Long?,
    val permissions: Set<String> = emptySet(),
) {
    companion object {
        fun from(
            project: Project,
            memberCount: Int,
        ): ProjectListItemResponse =
            ProjectListItemResponse(
                id = project.id,
                name = project.name,
                description = project.description,
                isPublic = project.isPublic,
                isActive = project.isActive,
                iconFileId = project.iconFileId,
                typeId = project.typeId,
                memberCount = memberCount,
                createdAt = project.createdAt,
                version = project.version,
            )
    }
}
