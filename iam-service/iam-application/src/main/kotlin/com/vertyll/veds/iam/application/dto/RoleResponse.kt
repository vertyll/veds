package com.vertyll.veds.iam.application.dto

data class RoleResponse(
    val id: Long,
    val name: String,
    val description: String?,
    val permissions: List<String> = emptyList(),
    val unrestricted: Boolean = false,
    val scope: String = "GLOBAL",
    val system: Boolean = false,
    val version: Long? = null,
)
