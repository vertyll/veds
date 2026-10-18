package com.vertyll.veds.mail.infrastructure.persistence.repository

import com.vertyll.veds.mail.infrastructure.persistence.entity.RolePermissionsJpaEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
internal interface RolePermissionsJpaRepository : JpaRepository<RolePermissionsJpaEntity, String>
