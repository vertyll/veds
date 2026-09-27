package com.vertyll.veds.translation.application.dto

data class PagedResponse<T>(
    val items: List<T>,
    val pagination: PaginationMeta,
)
