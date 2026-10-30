package com.vertyll.veds.task.application.service.command

import com.vertyll.veds.sharederror.ApiException
import com.vertyll.veds.task.application.command.BatchDeleteTasksCommand
import com.vertyll.veds.task.application.command.ChangeTaskStatusCommand
import com.vertyll.veds.task.application.command.CreateTaskCommand
import com.vertyll.veds.task.application.command.UpdateTaskCommand
import com.vertyll.veds.task.application.dto.Actor
import com.vertyll.veds.task.application.dto.TaskResponse
import com.vertyll.veds.task.application.port.inbound.command.TaskCommandUseCase
import com.vertyll.veds.task.application.port.outbound.TaskEventPublisherPort
import com.vertyll.veds.task.application.service.TaskAuthorizationService
import com.vertyll.veds.task.application.service.TaskReferenceValidator
import com.vertyll.veds.task.domain.error.TaskError
import com.vertyll.veds.task.domain.model.Task
import com.vertyll.veds.task.domain.model.TaskPermission
import com.vertyll.veds.task.domain.model.VersionGuard
import com.vertyll.veds.task.domain.repository.TaskCommentRepository
import com.vertyll.veds.task.domain.repository.TaskRepository
import com.vertyll.veds.task.domain.repository.UserDirectoryRepository
import java.util.UUID

@Suppress("LongParameterList")
class TaskCommandService(
    private val taskRepository: TaskRepository,
    private val commentRepository: TaskCommentRepository,
    private val userDirectory: UserDirectoryRepository,
    private val authorization: TaskAuthorizationService,
    private val references: TaskReferenceValidator,
    private val eventPublisher: TaskEventPublisherPort,
) : TaskCommandUseCase {
    override fun createTask(
        command: CreateTaskCommand,
        actor: Actor,
    ): TaskResponse {
        authorization.requireProjectPermission(projectId = command.projectId, actorId = actor.id, permission = TaskPermission.MANAGE_TASKS)
        references.validate(
            projectId = command.projectId,
            statusId = command.statusId,
            categoryIds = command.categoryIds,
            assigneeIds = command.assigneeIds,
        )

        userDirectory.save(actor.toUserRef())

        val task =
            taskRepository.save(
                Task.create(
                    projectId = command.projectId,
                    number = taskRepository.highestNumberIn(command.projectId) + 1,
                    name = command.name,
                    description = command.description,
                    priority = command.priority,
                    statusId = command.statusId,
                    categoryIds = command.categoryIds,
                    assigneeIds = command.assigneeIds,
                    createdBy = actor.id,
                    priceEstimation = command.priceEstimation,
                    accessRoleId = command.accessRoleId,
                    attachmentIds = command.attachmentIds,
                ),
            )

        eventPublisher.publishTaskCreated(
            taskId = task.id,
            projectId = task.projectId,
            name = task.name,
            createdBy = actor.id,
            assigneeIds = task.assigneeIds,
        )

        return TaskResponse.from(task)
    }

    override fun updateTask(
        taskId: UUID,
        command: UpdateTaskCommand,
        actor: Actor,
        version: Long?,
    ): TaskResponse {
        val task = authorization.requireTaskPermission(taskId, actor.id, TaskPermission.MANAGE_TASKS)

        VersionGuard.requireMatch(currentVersion = task.version, expectedVersion = version) { ApiException(TaskError.VERSION_MISMATCH) }
        references.validate(
            projectId = task.projectId,
            statusId = command.statusId,
            categoryIds = command.categoryIds,
            assigneeIds = command.assigneeIds,
        )

        val previousAssignees = task.assigneeIds
        val previousStatus = task.statusId

        val updated =
            taskRepository.save(
                task
                    .describe(newName = command.name, newDescription = command.description)
                    .reprioritise(command.priority)
                    .moveTo(command.statusId)
                    .categoriseAs(command.categoryIds)
                    .assignTo(command.assigneeIds)
                    .estimateAt(command.priceEstimation)
                    .restrictTo(command.accessRoleId)
                    .withAttachments(command.attachmentIds),
            )

        if (updated.assigneeIds != previousAssignees) {
            eventPublisher.publishTaskAssigned(
                taskId = updated.id,
                projectId = updated.projectId,
                assigneeIds = updated.assigneeIds,
                assignedBy = actor.id,
            )
        }
        if (updated.statusId != previousStatus) {
            eventPublisher.publishTaskStatusChanged(
                taskId = updated.id,
                projectId = updated.projectId,
                statusId = updated.statusId,
                changedBy = actor.id,
            )
        }

        return TaskResponse.from(updated)
    }

    override fun changeStatus(
        taskId: UUID,
        command: ChangeTaskStatusCommand,
        actor: Actor,
        version: Long?,
    ): TaskResponse {
        val task = authorization.requireTaskPermission(taskId, actor.id, TaskPermission.MANAGE_TASKS)

        VersionGuard.requireMatch(currentVersion = task.version, expectedVersion = version) { ApiException(TaskError.VERSION_MISMATCH) }
        references.validate(projectId = task.projectId, statusId = command.statusId, categoryIds = emptySet(), assigneeIds = emptySet())

        val moved = task.moveTo(command.statusId)
        if (moved === task) {
            return TaskResponse.from(task)
        }

        val saved = taskRepository.save(moved)
        eventPublisher.publishTaskStatusChanged(
            taskId = saved.id,
            projectId = saved.projectId,
            statusId = saved.statusId,
            changedBy = actor.id,
        )
        return TaskResponse.from(saved)
    }

    override fun archiveTask(
        taskId: UUID,
        actor: Actor,
        version: Long?,
    ) {
        val task = authorization.requireTaskPermission(taskId, actor.id, TaskPermission.MANAGE_TASKS)

        VersionGuard.requireMatch(currentVersion = task.version, expectedVersion = version) { ApiException(TaskError.VERSION_MISMATCH) }

        taskRepository.save(task.archive())
        commentRepository.deleteAllByTaskId(task.id)
        eventPublisher.publishTaskArchived(taskId = task.id, projectId = task.projectId)
    }

    override fun archiveTasks(
        command: BatchDeleteTasksCommand,
        actor: Actor,
    ): Int {
        val tasks =
            command.taskIds.map { id ->
                authorization.requireTaskPermission(taskId = id, actorId = actor.id, permission = TaskPermission.MANAGE_TASKS)
            }

        if (tasks.isEmpty()) return 0

        taskRepository.saveAll(tasks.map { it.archive() })
        tasks.forEach { task ->
            commentRepository.deleteAllByTaskId(task.id)
            eventPublisher.publishTaskArchived(taskId = task.id, projectId = task.projectId)
        }
        return tasks.size
    }
}
