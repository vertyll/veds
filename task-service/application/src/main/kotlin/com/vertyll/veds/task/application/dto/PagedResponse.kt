package com.vertyll.veds.task.application.dto

data class PagedResponse<T>(
    val items: List<T>,
    val pagination: PaginationMeta,
)
