package com.vertyll.veds.file.application.command

import java.util.UUID

data class ConfirmUploadCommand(
    val fileId: UUID,
)
