package com.vertyll.veds.task.infrastructure.persistence.repository

import com.vertyll.veds.task.infrastructure.persistence.entity.TaskCommentJpaEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
internal interface TaskCommentJpaRepository : JpaRepository<TaskCommentJpaEntity, UUID> {
    fun findAllByAttachmentIdsContaining(attachmentId: UUID): List<TaskCommentJpaEntity>

    fun findAllByTaskIdOrderByCreatedAtAsc(taskId: UUID): List<TaskCommentJpaEntity>

    fun deleteAllByTaskId(taskId: UUID)
}
