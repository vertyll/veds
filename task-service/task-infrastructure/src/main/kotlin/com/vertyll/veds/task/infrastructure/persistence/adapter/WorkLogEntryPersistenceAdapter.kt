package com.vertyll.veds.task.infrastructure.persistence.adapter

import com.vertyll.veds.task.domain.model.WorkLogEntry
import com.vertyll.veds.task.domain.repository.WorkLogEntryRepository
import com.vertyll.veds.task.infrastructure.persistence.entity.WorkLogEntryJpaEntity
import com.vertyll.veds.task.infrastructure.persistence.repository.WorkLogEntryJpaRepository
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Component
internal class WorkLogEntryPersistenceAdapter(
    private val repository: WorkLogEntryJpaRepository,
) : WorkLogEntryRepository {
    override fun save(entry: WorkLogEntry): WorkLogEntry = repository.save(entry.toJpaEntity()).toDomain()

    override fun findById(id: UUID): WorkLogEntry? = repository.findByIdOrNull(id)?.toDomain()

    @Transactional(readOnly = true)
    override fun findAllByTaskId(taskId: UUID): List<WorkLogEntry> =
        repository.findAllByTaskIdOrderByWorkedOnDescCreatedAtDesc(taskId).map { it.toDomain() }

    override fun deleteById(id: UUID) = repository.deleteById(id)

    @Transactional(readOnly = true)
    override fun sumMinutesByTaskId(taskId: UUID): Int = repository.sumMinutesByTaskId(taskId)
}

private fun WorkLogEntry.toJpaEntity() =
    WorkLogEntryJpaEntity(
        id = id,
        taskId = taskId,
        authorId = authorId,
        minutes = minutes,
        workedOn = workedOn,
        description = description,
        hidden = hidden,
        createdAt = createdAt,
        updatedAt = updatedAt,
        version = version,
    )

private fun WorkLogEntryJpaEntity.toDomain() =
    WorkLogEntry(
        id = id,
        taskId = taskId,
        authorId = authorId,
        minutes = minutes,
        workedOn = workedOn,
        description = description,
        hidden = hidden,
        createdAt = createdAt,
        updatedAt = updatedAt,
        version = version,
    )
