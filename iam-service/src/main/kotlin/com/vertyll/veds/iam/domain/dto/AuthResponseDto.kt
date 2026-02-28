package com.vertyll.veds.iam.domain.dto

data class AuthResponseDto(
    val token: String,
    val type: String,
    val expiresIn: Long,
)
