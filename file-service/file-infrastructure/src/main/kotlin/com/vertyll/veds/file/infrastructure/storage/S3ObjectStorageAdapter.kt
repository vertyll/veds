package com.vertyll.veds.file.infrastructure.storage

import com.vertyll.veds.file.application.dto.PresignedUrl
import com.vertyll.veds.file.application.port.outbound.ObjectStoragePort
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest
import software.amazon.awssdk.services.s3.model.GetObjectRequest
import software.amazon.awssdk.services.s3.model.HeadObjectRequest
import software.amazon.awssdk.services.s3.model.NoSuchKeyException
import software.amazon.awssdk.services.s3.model.PutObjectRequest
import software.amazon.awssdk.services.s3.presigner.S3Presigner
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest
import java.time.Instant

@Component
internal class S3ObjectStorageAdapter(
    private val s3Client: S3Client,
    private val presigner: S3Presigner,
    private val properties: ObjectStorageProperties,
) : ObjectStoragePort {
    private val logger = LoggerFactory.getLogger(javaClass)

    override fun presignUpload(
        objectKey: String,
        contentType: String,
    ): PresignedUrl {
        val putRequest =
            PutObjectRequest
                .builder()
                .bucket(properties.bucket)
                .key(objectKey)
                .contentType(contentType)
                .build()

        val presigned =
            presigner.presignPutObject(
                PutObjectPresignRequest
                    .builder()
                    .signatureDuration(properties.uploadUrlValidity)
                    .putObjectRequest(putRequest)
                    .build(),
            )

        return PresignedUrl(
            url = presigned.url().toString(),
            expiresAt = Instant.now().plus(properties.uploadUrlValidity),
        )
    }

    override fun presignDownload(
        objectKey: String,
        originalName: String,
        contentType: String,
    ): PresignedUrl {
        val getRequest =
            GetObjectRequest
                .builder()
                .bucket(properties.bucket)
                .key(objectKey)
                .responseContentType(contentType)
                .responseContentDisposition("attachment; filename=\"$originalName\"")
                .build()

        val presigned =
            presigner.presignGetObject(
                GetObjectPresignRequest
                    .builder()
                    .signatureDuration(properties.downloadUrlValidity)
                    .getObjectRequest(getRequest)
                    .build(),
            )

        return PresignedUrl(
            url = presigned.url().toString(),
            expiresAt = Instant.now().plus(properties.downloadUrlValidity),
        )
    }

    override fun sizeOf(objectKey: String): Long? =
        try {
            s3Client
                .headObject(
                    HeadObjectRequest
                        .builder()
                        .bucket(properties.bucket)
                        .key(objectKey)
                        .build(),
                ).contentLength()
        } catch (e: NoSuchKeyException) {
            logger.debug("Object {} is not in storage: {}", objectKey, e.message)
            null
        }

    override fun delete(objectKey: String) {
        s3Client.deleteObject(
            DeleteObjectRequest
                .builder()
                .bucket(properties.bucket)
                .key(objectKey)
                .build(),
        )
    }
}
