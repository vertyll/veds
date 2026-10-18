package com.vertyll.veds.task.application.port.inbound.command

import com.vertyll.veds.task.application.command.BatchDeleteTasksCommand
import com.vertyll.veds.task.application.command.ChangeTaskStatusCommand
import com.vertyll.veds.task.application.command.CreateTaskCommand
import com.vertyll.veds.task.application.command.UpdateTaskCommand
import com.vertyll.veds.task.application.dto.Actor
import com.vertyll.veds.task.application.dto.TaskResponse
import java.util.UUID

interface TaskCommandUseCase {
    fun createTask(
        command: CreateTaskCommand,
        actor: Actor,
    ): TaskResponse

    fun updateTask(
        taskId: UUID,
        command: UpdateTaskCommand,
        actor: Actor,
        version: Long? = null,
    ): TaskResponse

    fun changeStatus(
        taskId: UUID,
        command: ChangeTaskStatusCommand,
        actor: Actor,
        version: Long? = null,
    ): TaskResponse

    fun archiveTask(
        taskId: UUID,
        actor: Actor,
        version: Long? = null,
    )

    fun archiveTasks(
        command: BatchDeleteTasksCommand,
        actor: Actor,
    ): Int
}
