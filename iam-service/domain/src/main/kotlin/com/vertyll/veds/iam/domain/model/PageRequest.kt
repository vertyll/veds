package com.vertyll.veds.iam.domain.model

/**
 * Framework-agnostic pagination request used by domain repositories.
 *
 * Lives in the domain layer so that ports do not leak Spring Data
 * (`Pageable`) into modules that should not depend on Spring.
 */
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
