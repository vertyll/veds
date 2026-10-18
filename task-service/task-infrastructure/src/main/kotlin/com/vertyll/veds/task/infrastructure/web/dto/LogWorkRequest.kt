package com.vertyll.veds.task.infrastructure.web.dto

import com.vertyll.veds.task.application.command.LogWorkCommand
import com.vertyll.veds.task.domain.model.WorkLogEntry
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Positive
import jakarta.validation.constraints.Size
import java.time.LocalDate

data class LogWorkRequest(
    @field:Positive(message = "validation.task.work_log.minutes_positive")
    @field:Max(value = WorkLogEntry.MAX_MINUTES_PER_ENTRY.toLong(), message = "validation.task.work_log.minutes_too_large")
    val minutes: Int = 0,
    @field:NotNull(message = "validation.task.work_log.worked_on_required")
    val workedOn: LocalDate? = null,
    @field:Size(max = 500, message = "validation.task.work_log.description_too_long")
    val description: String? = null,
    val hidden: Boolean = false,
) {
    fun toCommand(): LogWorkCommand =
        LogWorkCommand(
            minutes = minutes,
            workedOn = requireNotNull(workedOn),
            description = description?.takeIf { it.isNotBlank() },
            hidden = hidden,
        )
}
