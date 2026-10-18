package com.vertyll.veds.project.application.dto

import java.util.UUID

data class ProjectDetailsResponse(
    val project: ProjectResponse,
    val type: ProjectTypeResponse?,
    val members: List<ProjectMemberResponse>,
    val categories: List<ProjectCategoryResponse>,
    val statuses: List<ProjectStatusResponse>,
    val permissions: Set<String>,
    val currentUserId: UUID,
)
