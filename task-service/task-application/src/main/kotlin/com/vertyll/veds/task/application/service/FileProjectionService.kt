package com.vertyll.veds.task.application.service

import com.vertyll.veds.task.application.port.inbound.FileProjectionUseCase
import com.vertyll.veds.task.application.port.outbound.UseCaseLogger
import com.vertyll.veds.task.domain.repository.TaskCommentRepository
import com.vertyll.veds.task.domain.repository.TaskRepository
import java.util.UUID

class FileProjectionService(
    private val taskRepository: TaskRepository,
    private val commentRepository: TaskCommentRepository,
    private val logger: UseCaseLogger,
) : FileProjectionUseCase {
    override fun fileDeleted(fileId: UUID) {
        val tasks = taskRepository.findAllByAttachmentId(fileId)
        if (tasks.isNotEmpty()) {
            taskRepository.saveAll(tasks.map { it.withoutAttachment(fileId) })
        }

        val comments = commentRepository.findAllByAttachmentId(fileId)
        if (comments.isNotEmpty()) {
            commentRepository.saveAll(comments.map { it.withoutAttachment(fileId) })
        }

        if (tasks.isNotEmpty() || comments.isNotEmpty()) {
            logger.info(
                "Dropped deleted file {} from {} tasks and {} comments",
                fileId,
                tasks.size,
                comments.size,
            )
        }
    }
}
