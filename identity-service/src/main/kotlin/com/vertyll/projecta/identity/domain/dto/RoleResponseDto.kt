package com.vertyll.projecta.identity.domain.dto

data class RoleResponseDto(
    val id: Long,
    val name: String,
    val description: String?,
    val version: Long? = null,
)
