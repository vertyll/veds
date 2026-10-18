package com.vertyll.veds.task.application.port.inbound.command

import com.vertyll.veds.task.application.command.LogWorkCommand
import com.vertyll.veds.task.application.command.UpdateWorkLogCommand
import com.vertyll.veds.task.application.dto.Actor
import com.vertyll.veds.task.application.dto.WorkLogEntryResponse
import java.util.UUID

interface WorkLogCommandUseCase {
    fun logWork(
        taskId: UUID,
        command: LogWorkCommand,
        actor: Actor,
    ): WorkLogEntryResponse

    fun editEntry(
        entryId: UUID,
        command: UpdateWorkLogCommand,
        actor: Actor,
        version: Long? = null,
    ): WorkLogEntryResponse

    fun deleteEntry(
        entryId: UUID,
        actor: Actor,
    )
}
