package com.vertyll.veds.iam.application.dto

data class PermissionModuleResponse(
    val module: String,
    val permissions: List<PermissionResponse>,
)
