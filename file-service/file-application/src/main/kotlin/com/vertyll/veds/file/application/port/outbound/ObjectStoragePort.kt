package com.vertyll.veds.file.application.port.outbound

import com.vertyll.veds.file.application.dto.PresignedUrl

interface ObjectStoragePort {
    fun presignUpload(
        objectKey: String,
        contentType: String,
    ): PresignedUrl

    fun presignDownload(
        objectKey: String,
        originalName: String,
        contentType: String,
    ): PresignedUrl

    fun sizeOf(objectKey: String): Long?

    fun delete(objectKey: String)
}
