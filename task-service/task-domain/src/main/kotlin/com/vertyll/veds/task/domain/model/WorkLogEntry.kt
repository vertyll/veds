@file:OptIn(ExperimentalUuidApi::class)

package com.vertyll.veds.task.domain.model

import java.time.Instant
import java.time.LocalDate
import java.util.UUID
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlin.uuid.toJavaUuid

data class WorkLogEntry(
    val id: UUID = Uuid.generateV7().toJavaUuid(),
    val taskId: UUID,
    val authorId: UUID,
    val minutes: Int,
    val workedOn: LocalDate,
    val description: String? = null,
    val hidden: Boolean = false,
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now(),
    val version: Long? = null,
) {
    init {
        require(minutes > 0) { "logged minutes must be positive" }
        require(minutes <= MAX_MINUTES_PER_ENTRY) { "a single entry must not exceed $MAX_MINUTES_PER_ENTRY minutes" }
        require(description == null || description.isNotBlank()) { "description must not be blank when given" }
    }

    fun editedBy(
        editorId: UUID,
        newMinutes: Int,
        newWorkedOn: LocalDate,
        newDescription: String?,
        newHidden: Boolean,
    ): WorkLogEntry {
        check(editorId == authorId) { "only the author may edit a work log entry" }
        return copy(
            minutes = newMinutes,
            workedOn = newWorkedOn,
            description = newDescription,
            hidden = newHidden,
            updatedAt = Instant.now(),
        )
    }

    fun isVisibleTo(
        userId: UUID,
        role: RolePermissionsRef?,
    ): Boolean = !hidden || isAuthoredBy(userId) || role?.grants(TaskPermission.VIEW_HIDDEN_WORK_LOG) == true

    fun isAuthoredBy(userId: UUID): Boolean = authorId == userId

    companion object {
        const val MAX_MINUTES_PER_ENTRY: Int = 24 * 60

        fun create(
            taskId: UUID,
            authorId: UUID,
            minutes: Int,
            workedOn: LocalDate,
            description: String? = null,
            hidden: Boolean = false,
        ): WorkLogEntry =
            WorkLogEntry(
                taskId = taskId,
                authorId = authorId,
                minutes = minutes,
                workedOn = workedOn,
                description = description,
                hidden = hidden,
            )
    }
}
