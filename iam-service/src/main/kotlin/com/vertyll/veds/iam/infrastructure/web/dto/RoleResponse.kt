package com.vertyll.veds.iam.infrastructure.web.dto

data class RoleResponse(
    val id: Long,
    val name: String,
    val description: String?,
    val version: Long? = null,
)
