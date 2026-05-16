package com.vertyll.veds.iam.domain.model

/**
 * Framework-agnostic paged result returned by domain repositories.
 */
data class PageResult<T>(
    val content: List<T>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
) {
    val totalPages: Int
        get() = if (size == 0) 0 else ((totalElements + size - 1) / size).toInt()

    fun <R> map(transform: (T) -> R): PageResult<R> = PageResult(content.map(transform), page, size, totalElements)
}
