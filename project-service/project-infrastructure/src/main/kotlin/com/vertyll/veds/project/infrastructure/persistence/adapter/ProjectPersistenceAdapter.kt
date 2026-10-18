package com.vertyll.veds.project.infrastructure.persistence.adapter

import com.vertyll.veds.project.domain.model.PageRequest
import com.vertyll.veds.project.domain.model.PageResult
import com.vertyll.veds.project.domain.model.Project
import com.vertyll.veds.project.domain.model.ProjectSearchCriteria
import com.vertyll.veds.project.domain.model.ProjectSortField
import com.vertyll.veds.project.domain.repository.ProjectRepository
import com.vertyll.veds.project.infrastructure.persistence.entity.ProjectJpaEntity
import com.vertyll.veds.project.infrastructure.persistence.repository.ProjectJpaRepository
import org.springframework.data.domain.Sort
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component
import java.util.UUID
import org.springframework.data.domain.PageRequest as SpringPageRequest

@Component
internal class ProjectPersistenceAdapter(
    private val repository: ProjectJpaRepository,
) : ProjectRepository {
    override fun save(project: Project): Project = repository.save(project.toJpaEntity()).toDomain()

    override fun findById(id: UUID): Project? = repository.findByIdOrNull(id)?.toDomain()

    override fun findAllByIds(ids: Collection<UUID>): List<Project> =
        if (ids.isEmpty()) emptyList() else repository.findAllById(ids).map { it.toDomain() }

    override fun search(
        criteria: ProjectSearchCriteria,
        pageRequest: PageRequest,
    ): PageResult<Project> {
        val page =
            repository.search(
                requesterId = criteria.requesterId,
                searchTerm = criteria.searchTerm,
                typeId = criteria.typeId,
                onlyActive = criteria.onlyActive,
                includePublic = criteria.includePublic,
                pageable =
                    SpringPageRequest.of(
                        pageRequest.page,
                        pageRequest.size,
                        criteria.toSort(),
                    ),
            )

        return PageResult(
            content = page.content.map { it.toDomain() },
            page = pageRequest.page,
            size = pageRequest.size,
            totalElements = page.totalElements,
        )
    }

    override fun existsById(id: UUID): Boolean = repository.existsById(id)

    override fun delete(id: UUID) = repository.deleteById(id)
}

private fun ProjectSearchCriteria.toSort(): Sort {
    val property =
        when (sortBy) {
            ProjectSortField.NAME -> "name"
            ProjectSortField.CREATED_AT -> "createdAt"
            ProjectSortField.UPDATED_AT -> "updatedAt"
        }
    return if (sortDescending) Sort.by(property).descending() else Sort.by(property).ascending()
}

private fun Project.toJpaEntity() =
    ProjectJpaEntity(
        id = this.id,
        name = this.name,
        description = this.description,
        isPublic = this.isPublic,
        iconFileId = this.iconFileId,
        typeId = this.typeId,
        ownerId = this.ownerId,
        hiddenWorkLogEnabled = this.hiddenWorkLogEnabled,
        isActive = this.isActive,
        createdAt = this.createdAt,
        updatedAt = this.updatedAt,
        version = this.version,
    )

internal fun ProjectJpaEntity.toDomain() =
    Project(
        id = this.id,
        name = this.name,
        description = this.description,
        isPublic = this.isPublic,
        iconFileId = this.iconFileId,
        typeId = this.typeId,
        ownerId = this.ownerId,
        hiddenWorkLogEnabled = this.hiddenWorkLogEnabled,
        isActive = this.isActive,
        createdAt = this.createdAt,
        updatedAt = this.updatedAt,
        version = this.version,
    )
