package com.vertyll.veds.template.application.dto

import com.vertyll.veds.template.domain.model.Template
import com.vertyll.veds.template.domain.model.TemplateStatus
import java.time.Instant

data class TemplateResponse(
    val id: String,
    val name: String,
    val payload: String,
    val status: TemplateStatus,
    val createdAt: Instant,
    val updatedAt: Instant,
) {
    companion object {
        fun from(template: Template): TemplateResponse =
            TemplateResponse(
                id = template.id,
                name = template.name,
                payload = template.payload,
                status = template.status,
                createdAt = template.createdAt,
                updatedAt = template.updatedAt,
            )
    }
}
