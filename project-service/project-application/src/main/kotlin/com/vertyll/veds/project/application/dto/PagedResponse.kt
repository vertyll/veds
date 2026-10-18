package com.vertyll.veds.project.application.dto

import com.vertyll.veds.project.domain.model.PageResult

data class PagedResponse<T>(
    val items: List<T>,
    val pagination: PaginationMeta,
) {
    companion object {
        fun <D, R> from(
            result: PageResult<D>,
            transform: (D) -> R,
        ): PagedResponse<R> =
            PagedResponse(
                items = result.content.map(transform),
                pagination =
                    PaginationMeta(
                        total = result.totalElements,
                        page = result.page,
                        pageSize = result.size,
                        totalPages = result.totalPages,
                        hasMore = result.page + 1 < result.totalPages,
                    ),
            )
    }
}
