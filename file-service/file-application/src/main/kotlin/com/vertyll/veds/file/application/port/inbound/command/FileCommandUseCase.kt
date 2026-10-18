package com.vertyll.veds.file.application.port.inbound.command

import com.vertyll.veds.file.application.command.AttachFileCommand
import com.vertyll.veds.file.application.command.ConfirmUploadCommand
import com.vertyll.veds.file.application.command.RequestUploadCommand
import com.vertyll.veds.file.application.dto.Actor
import com.vertyll.veds.file.application.dto.FileResponse
import com.vertyll.veds.file.application.dto.UploadTicketResponse
import java.util.UUID

interface FileCommandUseCase {
    fun requestUpload(
        command: RequestUploadCommand,
        actor: Actor,
    ): UploadTicketResponse

    fun confirmUpload(
        command: ConfirmUploadCommand,
        actor: Actor,
    ): FileResponse

    fun attach(
        command: AttachFileCommand,
        actor: Actor,
    ): FileResponse

    fun delete(
        fileId: UUID,
        actor: Actor,
    )

    fun purgeAbandonedUploads(): Int

    fun purgeDeletedObjects(): Int
}
