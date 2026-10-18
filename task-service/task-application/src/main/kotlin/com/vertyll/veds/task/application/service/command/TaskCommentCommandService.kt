package com.vertyll.veds.task.application.service.command

import com.vertyll.veds.sharederror.ApiException
import com.vertyll.veds.task.application.command.CreateCommentCommand
import com.vertyll.veds.task.application.command.UpdateCommentCommand
import com.vertyll.veds.task.application.dto.Actor
import com.vertyll.veds.task.application.dto.TaskCommentResponse
import com.vertyll.veds.task.application.dto.TaskUserView
import com.vertyll.veds.task.application.port.inbound.command.TaskCommentCommandUseCase
import com.vertyll.veds.task.application.port.outbound.TaskEventPublisherPort
import com.vertyll.veds.task.application.service.TaskAuthorizationService
import com.vertyll.veds.task.domain.error.TaskError
import com.vertyll.veds.task.domain.model.TaskComment
import com.vertyll.veds.task.domain.model.TaskPermission
import com.vertyll.veds.task.domain.model.VersionGuard
import com.vertyll.veds.task.domain.repository.TaskCommentRepository
import com.vertyll.veds.task.domain.repository.UserDirectoryRepository
import java.util.UUID

class TaskCommentCommandService(
    private val commentRepository: TaskCommentRepository,
    private val userDirectory: UserDirectoryRepository,
    private val authorization: TaskAuthorizationService,
    private val eventPublisher: TaskEventPublisherPort,
) : TaskCommentCommandUseCase {
    private companion object {
        private const val EXCERPT_LENGTH = 120
    }

    override fun addComment(
        taskId: UUID,
        command: CreateCommentCommand,
        actor: Actor,
    ): TaskCommentResponse {
        val task = authorization.requireTaskPermission(taskId, actor.id, TaskPermission.COMMENT)

        userDirectory.save(actor.toUserRef())

        val comment =
            commentRepository.save(
                TaskComment.create(
                    taskId = taskId,
                    authorId = actor.id,
                    content = command.content,
                    attachmentIds = command.attachmentIds,
                ),
            )

        eventPublisher.publishCommentAdded(
            taskId = taskId,
            projectId = task.projectId,
            commentId = comment.id,
            authorId = actor.id,
            excerpt = comment.content.take(EXCERPT_LENGTH),
        )

        return comment.toResponse(actor)
    }

    override fun editComment(
        commentId: UUID,
        command: UpdateCommentCommand,
        actor: Actor,
        version: Long?,
    ): TaskCommentResponse {
        val comment =
            commentRepository.findById(commentId)
                ?: throw ApiException(TaskError.COMMENT_NOT_FOUND, mapOf("commentId" to commentId.toString()))

        authorization.requireTaskPermission(comment.taskId, actor.id, TaskPermission.COMMENT)

        if (!comment.isAuthoredBy(actor.id)) {
            throw ApiException(TaskError.COMMENT_NOT_AUTHORED_BY_CALLER, mapOf("commentId" to commentId.toString()))
        }

        VersionGuard.requireMatch(comment.version, version) { ApiException(TaskError.VERSION_MISMATCH) }

        val edited = commentRepository.save(comment.editedBy(actor.id, command.content))
        return edited.toResponse(actor)
    }

    override fun deleteComment(
        commentId: UUID,
        actor: Actor,
    ) {
        val comment =
            commentRepository.findById(commentId)
                ?: throw ApiException(TaskError.COMMENT_NOT_FOUND, mapOf("commentId" to commentId.toString()))

        val task = authorization.requireTaskPermission(comment.taskId, actor.id, TaskPermission.COMMENT)
        val moderator =
            authorization
                .effectivePermissions(task.projectId, actor.id)
                .contains(TaskPermission.MANAGE_TASKS)

        if (!comment.isAuthoredBy(actor.id) && !moderator) {
            throw ApiException(TaskError.COMMENT_NOT_AUTHORED_BY_CALLER, mapOf("commentId" to commentId.toString()))
        }

        commentRepository.delete(commentId)
    }

    private fun TaskComment.toResponse(actor: Actor) =
        TaskCommentResponse(
            id = id,
            taskId = taskId,
            author =
                TaskUserView(
                    id = actor.id,
                    displayName = actor.toUserRef().displayName,
                    avatarFileId = null,
                ),
            content = content,
            attachmentIds = attachmentIds,
            createdAt = createdAt,
            updatedAt = updatedAt,
            version = version,
        )
}
