package com.vertyll.veds.template.infrastructure.persistence.repository

import com.vertyll.veds.template.infrastructure.persistence.entity.TemplateJpaEntity
import org.springframework.data.jpa.repository.JpaRepository

internal interface TemplateJpaRepository : JpaRepository<TemplateJpaEntity, String>
