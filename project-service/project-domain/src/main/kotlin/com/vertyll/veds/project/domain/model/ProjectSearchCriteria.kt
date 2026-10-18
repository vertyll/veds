package com.vertyll.veds.project.domain.model

import java.util.UUID

data class ProjectSearchCriteria(
    val requesterId: UUID,
    val searchTerm: String? = null,
    val typeId: UUID? = null,
    val onlyActive: Boolean = true,
    val includePublic: Boolean = true,
    val sortBy: ProjectSortField = ProjectSortField.CREATED_AT,
    val sortDescending: Boolean = true,
)
