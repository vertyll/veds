package com.vertyll.veds.file.application.dto

import java.time.Instant
import java.util.UUID

data class DownloadTicketResponse(
    val fileId: UUID,
    val downloadUrl: String,
    val originalName: String,
    val contentType: String,
    val expiresAt: Instant,
)
