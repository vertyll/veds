package com.vertyll.veds.file.infrastructure.storage

import jakarta.validation.constraints.NotBlank
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.validation.annotation.Validated
import java.time.Duration

@ConfigurationProperties(prefix = "application.file.storage")
@Validated
data class ObjectStorageProperties(
    @field:NotBlank
    val endpoint: String,
    @field:NotBlank
    val region: String,
    @field:NotBlank
    val bucket: String,
    @field:NotBlank
    val accessKey: String,
    @field:NotBlank
    val secretKey: String,
    val uploadUrlValidity: Duration = Duration.ofMinutes(DEFAULT_UPLOAD_URL_MINUTES),
    val downloadUrlValidity: Duration = Duration.ofMinutes(DEFAULT_DOWNLOAD_URL_MINUTES),
) {
    init {
        require(uploadUrlValidity > Duration.ZERO) { "application.file.storage.upload-url-validity must be positive" }
        require(downloadUrlValidity > Duration.ZERO) { "application.file.storage.download-url-validity must be positive" }
    }

    private companion object {
        private const val DEFAULT_UPLOAD_URL_MINUTES = 15L
        private const val DEFAULT_DOWNLOAD_URL_MINUTES = 5L
    }
}
