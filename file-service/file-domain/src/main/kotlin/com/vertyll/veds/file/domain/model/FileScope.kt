package com.vertyll.veds.file.domain.model

enum class FileScope(
    val maxSizeBytes: Long,
    val allowedContentTypes: Set<String>,
) {
    USER_AVATAR(
        maxSizeBytes = 2L * MEGABYTE,
        allowedContentTypes = IMAGE_TYPES,
    ),
    PROJECT_ICON(
        maxSizeBytes = 2L * MEGABYTE,
        allowedContentTypes = IMAGE_TYPES,
    ),

    TASK_ATTACHMENT(
        maxSizeBytes = 25L * MEGABYTE,
        allowedContentTypes = emptySet(),
    ),
    ;

    fun permits(contentType: String): Boolean = allowedContentTypes.isEmpty() || contentType.lowercase() in allowedContentTypes

    fun permitsSize(sizeBytes: Long): Boolean = sizeBytes in 1..maxSizeBytes
}

private const val MEGABYTE = 1024L * 1024L

private val IMAGE_TYPES = setOf("image/png", "image/jpeg", "image/webp", "image/gif")
