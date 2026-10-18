package com.vertyll.veds.task.infrastructure.persistence.repository

import com.vertyll.veds.task.infrastructure.persistence.entity.WorkLogEntryJpaEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.util.UUID

internal interface WorkLogEntryJpaRepository : JpaRepository<WorkLogEntryJpaEntity, UUID> {
    fun findAllByTaskIdOrderByWorkedOnDescCreatedAtDesc(taskId: UUID): List<WorkLogEntryJpaEntity>

    @Query("SELECT COALESCE(SUM(e.minutes), 0) FROM WorkLogEntryJpaEntity e WHERE e.taskId = :taskId")
    fun sumMinutesByTaskId(
        @Param("taskId") taskId: UUID,
    ): Int
}
