package com.vertyll.veds.task.application

import com.vertyll.veds.task.domain.model.WorkLogEntry
import com.vertyll.veds.task.domain.repository.WorkLogEntryRepository
import java.util.UUID

internal class InMemoryWorkLogRepository : WorkLogEntryRepository {
    val stored = linkedMapOf<UUID, WorkLogEntry>()

    override fun save(entry: WorkLogEntry) = entry.also { stored[it.id] = it }

    override fun findById(id: UUID) = stored[id]

    override fun findAllByTaskId(taskId: UUID) = stored.values.filter { it.taskId == taskId }

    override fun deleteById(id: UUID) {
        stored.remove(id)
    }

    override fun sumMinutesByTaskId(taskId: UUID) = stored.values.filter { it.taskId == taskId }.sumOf { it.minutes }
}
