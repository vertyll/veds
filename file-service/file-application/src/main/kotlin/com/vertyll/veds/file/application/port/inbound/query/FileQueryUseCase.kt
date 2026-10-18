package com.vertyll.veds.file.application.port.inbound.query

import com.vertyll.veds.file.application.dto.Actor
import com.vertyll.veds.file.application.dto.DownloadTicketResponse
import com.vertyll.veds.file.application.dto.FileResponse
import java.util.UUID

interface FileQueryUseCase {
    fun getFile(
        fileId: UUID,
        actor: Actor,
    ): FileResponse

    fun requestDownload(
        fileId: UUID,
        actor: Actor,
    ): DownloadTicketResponse

    fun listForScope(
        scopeId: UUID,
        actor: Actor,
    ): List<FileResponse>
}
