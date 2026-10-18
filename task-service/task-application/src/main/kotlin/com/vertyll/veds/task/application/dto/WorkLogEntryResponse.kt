package com.vertyll.veds.task.application.dto

import com.vertyll.veds.task.domain.model.WorkLogEntry
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

data class WorkLogEntryResponse(
    val id: UUID,
    val taskId: UUID,
    val author: TaskUserView,
    val minutes: Int,
    val workedOn: LocalDate,
    val description: String?,
    val hidden: Boolean,
    val createdAt: Instant,
    val updatedAt: Instant,
    val version: Long?,
) {
    companion object {
        fun from(
            entry: WorkLogEntry,
            author: TaskUserView,
        ): WorkLogEntryResponse =
            WorkLogEntryResponse(
                id = entry.id,
                taskId = entry.taskId,
                author = author,
                minutes = entry.minutes,
                workedOn = entry.workedOn,
                description = entry.description,
                hidden = entry.hidden,
                createdAt = entry.createdAt,
                updatedAt = entry.updatedAt,
                version = entry.version,
            )
    }
}
