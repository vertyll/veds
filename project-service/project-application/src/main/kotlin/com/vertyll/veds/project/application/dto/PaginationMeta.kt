package com.vertyll.veds.project.application.dto

data class PaginationMeta(
    val total: Long,
    val page: Int,
    val pageSize: Int,
    val totalPages: Int,
    val hasMore: Boolean,
)
