package com.vertyll.veds.task.infrastructure.persistence.entity

import jakarta.persistence.CollectionTable
import jakarta.persistence.Column
import jakarta.persistence.ElementCollection
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.JoinColumn
import jakarta.persistence.Table
import jakarta.persistence.Version
import java.time.Instant
import java.util.UUID

@Entity
@Table(
    name = "task_comment",
    indexes = [
        Index(name = "idx_task_comment_task_id", columnList = "task_id"),
        Index(name = "idx_task_comment_author_id", columnList = "author_id"),
    ],
)
internal class TaskCommentJpaEntity(
    @Id
    @Column(name = "id", nullable = false, updatable = false)
    var id: UUID,
    @Column(name = "task_id", nullable = false)
    var taskId: UUID,
    @Column(name = "author_id", nullable = false)
    var authorId: UUID,
    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    var content: String,
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "task_comment_attachment", joinColumns = [JoinColumn(name = "comment_id")])
    @Column(name = "attachment_id", nullable = false)
    var attachmentIds: MutableSet<UUID> = mutableSetOf(),
    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: Instant = Instant.now(),
    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now(),
    @Version
    @Column(name = "version")
    var version: Long? = null,
)
