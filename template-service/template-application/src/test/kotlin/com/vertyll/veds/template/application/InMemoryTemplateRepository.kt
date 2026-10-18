package com.vertyll.veds.template.application

import com.vertyll.veds.template.domain.model.Template
import com.vertyll.veds.template.domain.repository.TemplateRepository

internal class InMemoryTemplateRepository : TemplateRepository {
    val stored = linkedMapOf<String, Template>()

    var saveFails: Exception? = null

    fun given(vararg templates: Template) = templates.forEach { stored[it.id] = it }

    override fun save(template: Template): Template {
        saveFails?.let { throw it }
        return template.also { stored[it.id] = it }
    }

    override fun findById(id: String) = stored[id]

    override fun deleteById(id: String) {
        stored.remove(id)
    }
}
