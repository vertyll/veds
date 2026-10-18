package com.vertyll.veds.project.infrastructure.persistence.repository

import com.vertyll.veds.project.infrastructure.persistence.entity.ProjectJpaEntity
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
internal interface ProjectJpaRepository : JpaRepository<ProjectJpaEntity, UUID> {
    @Query(
        """
        SELECT p FROM ProjectJpaEntity p
        WHERE (
            p.ownerId = :requesterId
            OR EXISTS (
                SELECT 1 FROM ProjectMemberJpaEntity m
                WHERE m.projectId = p.id AND m.userId = :requesterId
            )
            OR (:includePublic = TRUE AND p.isPublic = TRUE)
        )
        AND (:onlyActive = FALSE OR p.isActive = TRUE)
        AND (:typeId IS NULL OR p.typeId = :typeId)
        AND (
            :searchTerm IS NULL
            OR LOWER(p.name) LIKE LOWER(CONCAT('%', CAST(:searchTerm AS string), '%'))
            OR LOWER(COALESCE(p.description, '')) LIKE LOWER(CONCAT('%', CAST(:searchTerm AS string), '%'))
        )
        """,
    )
    fun search(
        @Param("requesterId") requesterId: UUID,
        @Param("searchTerm") searchTerm: String?,
        @Param("typeId") typeId: UUID?,
        @Param("onlyActive") onlyActive: Boolean,
        @Param("includePublic") includePublic: Boolean,
        pageable: Pageable,
    ): Page<ProjectJpaEntity>
}
