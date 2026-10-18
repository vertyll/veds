package com.vertyll.veds.iam.application.dto

data class PermissionResponse(
    val id: Long,
    val name: String,
    val module: String,
    val scope: String,
    val description: String?,
    val grantedByRoles: List<String>,
)
