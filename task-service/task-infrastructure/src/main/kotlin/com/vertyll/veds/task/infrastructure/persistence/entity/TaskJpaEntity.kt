package com.vertyll.veds.task.infrastructure.persistence.entity

import com.vertyll.veds.task.domain.model.TaskPriority
import jakarta.persistence.CollectionTable
import jakarta.persistence.Column
import jakarta.persistence.ElementCollection
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
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
    name = "task",
    indexes = [
        Index(name = "idx_task_project_id", columnList = "project_id"),
        Index(name = "idx_task_status_id", columnList = "status_id"),
        Index(name = "idx_task_created_by", columnList = "created_by"),
        Index(name = "idx_task_is_active", columnList = "is_active"),
    ],
)
internal class TaskJpaEntity(
    @Id
    @Column(name = "id", nullable = false, updatable = false)
    var id: UUID,
    @Column(name = "project_id", nullable = false)
    var projectId: UUID,
    @Column(name = "number", nullable = false, updatable = false)
    var number: Int,
    @Column(name = "name", nullable = false, columnDefinition = "TEXT")
    var name: String,
    @Column(name = "description", columnDefinition = "TEXT")
    var description: String? = null,
    @Column(name = "price_estimation", nullable = false)
    var priceEstimation: Int = 0,
    @Column(name = "worked_minutes", nullable = false)
    var workedMinutes: Int = 0,
    @Enumerated(EnumType.STRING)
    @Column(name = "priority", nullable = false, length = 16)
    var priority: TaskPriority = TaskPriority.MEDIUM,
    @Column(name = "status_id")
    var statusId: UUID? = null,
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "task_category", joinColumns = [JoinColumn(name = "task_id")])
    @Column(name = "category_id", nullable = false)
    var categoryIds: MutableSet<UUID> = mutableSetOf(),
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "task_assignee", joinColumns = [JoinColumn(name = "task_id")])
    @Column(name = "user_id", nullable = false)
    var assigneeIds: MutableSet<UUID> = mutableSetOf(),
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "task_attachment", joinColumns = [JoinColumn(name = "task_id")])
    @Column(name = "attachment_id", nullable = false)
    var attachmentIds: MutableSet<UUID> = mutableSetOf(),
    @Column(name = "access_role_id")
    var accessRoleId: UUID? = null,
    @Column(name = "created_by", nullable = false)
    var createdBy: UUID,
    @Column(name = "is_active", nullable = false)
    var isActive: Boolean = true,
    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: Instant = Instant.now(),
    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now(),
    @Version
    @Column(name = "version")
    var version: Long? = null,
)
