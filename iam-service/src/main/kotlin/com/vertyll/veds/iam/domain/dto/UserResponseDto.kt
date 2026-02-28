package com.vertyll.veds.iam.domain.dto

data class UserResponseDto(
    val id: Long,
    val firstName: String,
    val lastName: String,
    val email: String,
    val roles: Set<String>,
    val profilePicture: String? = null,
    val phoneNumber: String? = null,
    val address: String? = null,
    val createdAt: String? = null,
    val updatedAt: String? = null,
    val version: Long? = null,
)
