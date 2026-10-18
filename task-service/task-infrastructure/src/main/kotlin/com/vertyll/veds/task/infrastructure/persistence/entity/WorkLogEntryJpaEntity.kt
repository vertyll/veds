package com.vertyll.veds.task.infrastructure.persistence.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import jakarta.persistence.Version
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

@Entity
@Table(
    name = "task_work_log",
    indexes = [
        Index(name = "idx_task_work_log_task_id", columnList = "task_id, worked_on"),
        Index(name = "idx_task_work_log_author_id", columnList = "author_id"),
    ],
)
internal class WorkLogEntryJpaEntity(
    @Id
    @Column(name = "id", nullable = false, updatable = false)
    var id: UUID,
    @Column(name = "task_id", nullable = false)
    var taskId: UUID,
    @Column(name = "author_id", nullable = false)
    var authorId: UUID,
    @Column(name = "minutes", nullable = false)
    var minutes: Int,
    @Column(name = "worked_on", nullable = false)
    var workedOn: LocalDate,
    @Column(name = "description", columnDefinition = "TEXT")
    var description: String? = null,
    @Column(name = "hidden", nullable = false)
    var hidden: Boolean = false,
    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: Instant = Instant.now(),
    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now(),
    @Version
    @Column(name = "version")
    var version: Long? = null,
)
