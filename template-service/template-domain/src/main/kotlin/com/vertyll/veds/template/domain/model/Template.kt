@file:OptIn(ExperimentalUuidApi::class)

package com.vertyll.veds.template.domain.model

import java.time.Instant
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

data class Template(
    val id: String = Uuid.generateV7().toString(),
    val name: String,
    val payload: String,
    val status: TemplateStatus = TemplateStatus.CREATED,
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now(),
) {
    fun markProcessed(): Template = copy(status = TemplateStatus.PROCESSED, updatedAt = Instant.now())
}
