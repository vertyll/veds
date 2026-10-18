package com.vertyll.veds.task.application.port.inbound

import java.util.UUID

fun interface FileProjectionUseCase {
    fun fileDeleted(fileId: UUID)
}
