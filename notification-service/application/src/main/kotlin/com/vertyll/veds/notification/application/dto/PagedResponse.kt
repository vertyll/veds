package com.vertyll.veds.notification.application.dto

data class PagedResponse<T>(
    val items: List<T>,
    val pagination: PaginationMeta,
)
