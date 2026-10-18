package com.vertyll.veds.task.infrastructure.persistence.repository

import com.vertyll.veds.task.infrastructure.persistence.entity.TaskJpaEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
internal interface TaskJpaRepository : JpaRepository<TaskJpaEntity, UUID> {
    fun findAllByProjectId(projectId: UUID): List<TaskJpaEntity>

    @Query("SELECT t FROM TaskJpaEntity t WHERE :categoryId MEMBER OF t.categoryIds")
    fun findAllByCategoryId(
        @Param("categoryId") categoryId: UUID,
    ): List<TaskJpaEntity>

    fun findAllByStatusId(statusId: UUID): List<TaskJpaEntity>

    fun findAllByAttachmentIdsContaining(attachmentId: UUID): List<TaskJpaEntity>

    @Query("SELECT COALESCE(MAX(t.number), 0) FROM TaskJpaEntity t WHERE t.projectId = :projectId")
    fun highestNumberIn(projectId: UUID): Int
}
