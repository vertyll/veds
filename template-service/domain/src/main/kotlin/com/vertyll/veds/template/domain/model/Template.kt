package com.vertyll.veds.template.domain.model

import java.time.Instant
import java.util.UUID

/**
 * Pure domain model — placeholder aggregate for template-service.
 *
 * Replace this class with your actual domain aggregate when cloning the service.
 * It must remain free of framework annotations (no JPA, no Spring, no Jackson).
 */
data class Template(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val payload: String,
    val status: TemplateStatus = TemplateStatus.CREATED,
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now(),
) {
    fun markProcessed(): Template = copy(status = TemplateStatus.PROCESSED, updatedAt = Instant.now())

    fun markFailed(): Template = copy(status = TemplateStatus.FAILED, updatedAt = Instant.now())
}
