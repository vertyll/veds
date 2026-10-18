package com.vertyll.veds.file.application.command

import java.util.UUID

data class AttachFileCommand(
    val fileId: UUID,
    val scopeId: UUID,
)
