package com.vertyll.veds.file.application.command

import com.vertyll.veds.file.domain.model.FileScope
import java.util.UUID

data class RequestUploadCommand(
    val originalName: String,
    val contentType: String,
    val declaredSizeBytes: Long,
    val scope: FileScope,
    val scopeId: UUID?,
)
