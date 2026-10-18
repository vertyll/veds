package com.vertyll.veds.file.infrastructure.storage

import org.springframework.boot.context.properties.ConfigurationProperties
import java.time.Duration

@ConfigurationProperties(prefix = "application.file.storage")
data class ObjectStorageProperties(
    val endpoint: String,
    val region: String,
    val bucket: String,
    val accessKey: String,
    val secretKey: String,
    val uploadUrlValidity: Duration = Duration.ofMinutes(DEFAULT_UPLOAD_URL_MINUTES),
    val downloadUrlValidity: Duration = Duration.ofMinutes(DEFAULT_DOWNLOAD_URL_MINUTES),
) {
    companion object {
        const val DEFAULT_UPLOAD_URL_MINUTES = 15L
        const val DEFAULT_DOWNLOAD_URL_MINUTES = 5L
    }
}
