package com.vertyll.veds.template.infrastructure.persistence.adapter

import com.vertyll.veds.template.domain.model.Template
import com.vertyll.veds.template.domain.repository.TemplateRepository
import com.vertyll.veds.template.infrastructure.persistence.entity.TemplateJpaEntity
import com.vertyll.veds.template.infrastructure.persistence.repository.TemplateJpaRepository
import org.springframework.stereotype.Component

@Component
internal class TemplatePersistenceAdapter(
    private val jpaRepository: TemplateJpaRepository,
) : TemplateRepository {
    override fun save(template: Template): Template = jpaRepository.save(template.toEntity()).toDomain()

    override fun findById(id: String): Template? = jpaRepository.findById(id).orElse(null)?.toDomain()

    override fun deleteById(id: String) {
        jpaRepository.deleteById(id)
    }
}

private fun Template.toEntity(): TemplateJpaEntity =
    TemplateJpaEntity(
        id = id,
        name = name,
        payload = payload,
        status = status,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

private fun TemplateJpaEntity.toDomain(): Template =
    Template(
        id = id,
        name = name,
        payload = payload,
        status = status,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )
