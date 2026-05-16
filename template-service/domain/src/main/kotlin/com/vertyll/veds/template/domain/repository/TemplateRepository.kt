package com.vertyll.veds.template.domain.repository

import com.vertyll.veds.template.domain.model.Template

interface TemplateRepository {
    fun save(template: Template): Template

    fun findById(id: String): Template?

    fun deleteById(id: String)
}
