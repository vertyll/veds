package com.vertyll.veds.iam.application.dto

data class RoleResponse(
    val id: Long,
    val name: String,
    val description: String?,
    val version: Long? = null,
)
