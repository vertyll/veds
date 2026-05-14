package com.vertyll.veds.template.domain.repository

import com.vertyll.veds.template.domain.model.Template

/**
 * Outbound port for persisting Template aggregates.
 *
 * Implemented by an adapter in `infrastructure/persistence/adapter/`.
 */
interface TemplateRepository {
    fun save(template: Template): Template

    fun findById(id: String): Template?

    fun deleteById(id: String)
}
