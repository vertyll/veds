package com.vertyll.veds.project.application.dto

import com.vertyll.veds.project.domain.model.ProjectSortField
import java.util.UUID

data class ProjectSearchParams(
    val searchTerm: String? = null,
    val typeId: UUID? = null,
    val onlyActive: Boolean = true,
    val includePublic: Boolean = true,
    val sortBy: ProjectSortField = ProjectSortField.CREATED_AT,
    val sortDescending: Boolean = true,
    val page: Int = 0,
    val size: Int = DEFAULT_PAGE_SIZE,
) {
    companion object {
        const val DEFAULT_PAGE_SIZE = 20
    }
}
