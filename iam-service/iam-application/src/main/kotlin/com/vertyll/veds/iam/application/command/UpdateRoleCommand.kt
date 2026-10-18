package com.vertyll.veds.iam.application.command

data class UpdateRoleCommand(
    val description: String?,
    val permissions: Set<String>,
)
