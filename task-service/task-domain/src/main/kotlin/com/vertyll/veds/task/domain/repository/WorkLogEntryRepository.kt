package com.vertyll.veds.task.domain.repository

import com.vertyll.veds.task.domain.model.WorkLogEntry
import java.util.UUID

interface WorkLogEntryRepository {
    fun save(entry: WorkLogEntry): WorkLogEntry

    fun findById(id: UUID): WorkLogEntry?

    fun findAllByTaskId(taskId: UUID): List<WorkLogEntry>

    fun deleteById(id: UUID)

    fun sumMinutesByTaskId(taskId: UUID): Int
}
