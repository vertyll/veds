package com.vertyll.veds.task.application.service.command

import com.vertyll.veds.sharederror.ApiException
import com.vertyll.veds.task.application.command.LogWorkCommand
import com.vertyll.veds.task.application.command.UpdateWorkLogCommand
import com.vertyll.veds.task.application.dto.Actor
import com.vertyll.veds.task.application.dto.TaskUserView
import com.vertyll.veds.task.application.dto.WorkLogEntryResponse
import com.vertyll.veds.task.application.port.inbound.command.WorkLogCommandUseCase
import com.vertyll.veds.task.application.service.TaskAuthorizationService
import com.vertyll.veds.task.domain.error.TaskError
import com.vertyll.veds.task.domain.model.TaskPermission
import com.vertyll.veds.task.domain.model.VersionGuard
import com.vertyll.veds.task.domain.model.WorkLogEntry
import com.vertyll.veds.task.domain.repository.ProjectDirectoryRepository
import com.vertyll.veds.task.domain.repository.TaskRepository
import com.vertyll.veds.task.domain.repository.UserDirectoryRepository
import com.vertyll.veds.task.domain.repository.WorkLogEntryRepository
import java.util.UUID

class WorkLogCommandService(
    private val entryRepository: WorkLogEntryRepository,
    private val taskRepository: TaskRepository,
    private val userDirectory: UserDirectoryRepository,
    private val projectDirectory: ProjectDirectoryRepository,
    private val authorization: TaskAuthorizationService,
) : WorkLogCommandUseCase {
    override fun logWork(
        taskId: UUID,
        command: LogWorkCommand,
        actor: Actor,
    ): WorkLogEntryResponse {
        val task = authorization.requireTaskPermission(taskId, actor.id, TaskPermission.LOG_WORK)
        val project =
            projectDirectory.findProject(task.projectId)
                ?: throw ApiException(TaskError.PROJECT_NOT_KNOWN, mapOf("projectId" to task.projectId.toString()))

        userDirectory.save(actor.toUserRef())

        val entry =
            entryRepository.save(
                WorkLogEntry.create(
                    taskId = taskId,
                    authorId = actor.id,
                    minutes = command.minutes,
                    workedOn = command.workedOn,
                    description = command.description,
                    hidden = command.hidden && project.hiddenWorkLogEnabled,
                ),
            )

        refreshTaskTotal(taskId)
        return entry.toResponse(actor)
    }

    override fun editEntry(
        entryId: UUID,
        command: UpdateWorkLogCommand,
        actor: Actor,
        version: Long?,
    ): WorkLogEntryResponse {
        val entry = ownedBy(actor, entryId)
        VersionGuard.requireMatch(entry.version, version) { ApiException(TaskError.VERSION_MISMATCH) }

        val edited =
            entryRepository.save(
                entry.editedBy(
                    editorId = actor.id,
                    newMinutes = command.minutes,
                    newWorkedOn = command.workedOn,
                    newDescription = command.description,
                    newHidden = command.hidden && hiddenWorkLogAllowedFor(entry.taskId),
                ),
            )

        refreshTaskTotal(entry.taskId)
        return edited.toResponse(actor)
    }

    override fun deleteEntry(
        entryId: UUID,
        actor: Actor,
    ) {
        val entry = ownedBy(actor, entryId)
        entryRepository.deleteById(entryId)
        refreshTaskTotal(entry.taskId)
    }

    private fun hiddenWorkLogAllowedFor(taskId: UUID): Boolean {
        val task =
            taskRepository.findById(taskId)
                ?: throw ApiException(TaskError.TASK_NOT_FOUND, mapOf("taskId" to taskId.toString()))
        val project =
            projectDirectory.findProject(task.projectId)
                ?: throw ApiException(TaskError.PROJECT_NOT_KNOWN, mapOf("projectId" to task.projectId.toString()))
        return project.hiddenWorkLogEnabled
    }

    private fun ownedBy(
        actor: Actor,
        entryId: UUID,
    ): WorkLogEntry {
        val entry =
            entryRepository.findById(entryId)
                ?: throw ApiException(TaskError.WORK_LOG_NOT_FOUND, mapOf("entryId" to entryId.toString()))

        authorization.requireTaskPermission(entry.taskId, actor.id, TaskPermission.LOG_WORK)

        if (!entry.isAuthoredBy(actor.id)) {
            throw ApiException(
                TaskError.WORK_LOG_NOT_AUTHORED_BY_CALLER,
                mapOf("entryId" to entryId.toString()),
            )
        }
        return entry
    }

    private fun refreshTaskTotal(taskId: UUID) {
        val task =
            taskRepository.findById(taskId)
                ?: throw ApiException(TaskError.TASK_NOT_FOUND, mapOf("taskId" to taskId.toString()))

        taskRepository.save(task.withWorkedMinutes(entryRepository.sumMinutesByTaskId(taskId)))
    }

    private fun WorkLogEntry.toResponse(actor: Actor) =
        WorkLogEntryResponse.from(
            entry = this,
            author =
                TaskUserView(
                    id = actor.id,
                    displayName = actor.toUserRef().displayName,
                    avatarFileId = null,
                ),
        )
}
