package com.vertyll.veds.iam.domain.model

data class PageRequest(
    val page: Int,
    val size: Int,
) {
    init {
        require(page >= 0) { "page must be >= 0" }
        require(size > 0) { "size must be > 0" }
    }

    val offset: Long get() = page.toLong() * size.toLong()
}
