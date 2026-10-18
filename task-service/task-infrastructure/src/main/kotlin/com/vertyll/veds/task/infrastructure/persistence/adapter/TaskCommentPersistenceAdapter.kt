package com.vertyll.veds.task.infrastructure.persistence.adapter

import com.vertyll.veds.task.domain.model.TaskComment
import com.vertyll.veds.task.domain.repository.TaskCommentRepository
import com.vertyll.veds.task.infrastructure.persistence.entity.TaskCommentJpaEntity
import com.vertyll.veds.task.infrastructure.persistence.repository.TaskCommentJpaRepository
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Component
internal class TaskCommentPersistenceAdapter(
    private val repository: TaskCommentJpaRepository,
) : TaskCommentRepository {
    override fun save(comment: TaskComment): TaskComment = repository.save(comment.toJpaEntity()).toDomain()

    override fun findById(id: UUID): TaskComment? = repository.findByIdOrNull(id)?.toDomain()

    override fun saveAll(comments: Collection<TaskComment>): List<TaskComment> =
        repository.saveAll(comments.map { it.toJpaEntity() }).map { it.toDomain() }

    override fun findAllByAttachmentId(attachmentId: UUID): List<TaskComment> =
        repository.findAllByAttachmentIdsContaining(attachmentId).map { it.toDomain() }

    override fun findAllByTaskId(taskId: UUID): List<TaskComment> =
        repository.findAllByTaskIdOrderByCreatedAtAsc(taskId).map { it.toDomain() }

    override fun delete(id: UUID) = repository.deleteById(id)

    @Transactional
    override fun deleteAllByTaskId(taskId: UUID) = repository.deleteAllByTaskId(taskId)
}

private fun TaskComment.toJpaEntity() =
    TaskCommentJpaEntity(
        id = this.id,
        taskId = this.taskId,
        authorId = this.authorId,
        content = this.content,
        attachmentIds = this.attachmentIds.toMutableSet(),
        createdAt = this.createdAt,
        updatedAt = this.updatedAt,
        version = this.version,
    )

internal fun TaskCommentJpaEntity.toDomain() =
    TaskComment(
        id = this.id,
        taskId = this.taskId,
        authorId = this.authorId,
        content = this.content,
        attachmentIds = this.attachmentIds.toSet(),
        createdAt = this.createdAt,
        updatedAt = this.updatedAt,
        version = this.version,
    )
