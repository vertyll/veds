package com.vertyll.projecta.identity.domain.dto

data class AuthResponseDto(
    val token: String,
    val type: String,
    val expiresIn: Long,
)
