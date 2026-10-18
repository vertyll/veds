package com.vertyll.veds.translation.domain.model

import java.time.Instant

data class TranslationKey(
    val key: String,
    val sourceService: String,
    val description: String? = null,
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now(),
) {
    init {
        require(key.isNotBlank()) { "translation key must not be blank" }
        require(PATTERN.matches(key)) { "'$key' is not a valid translation key" }
        require(sourceService.isNotBlank()) { "source service must not be blank" }
    }

    fun redeclaredBy(
        service: String,
        newDescription: String?,
    ): TranslationKey {
        check(service == sourceService) {
            "key '$key' is owned by '$sourceService' and cannot be re-declared by '$service'"
        }
        return copy(description = newDescription ?: description, updatedAt = Instant.now())
    }

    private companion object {
        private val PATTERN = Regex("^[a-z][a-z0-9]*(\\.[a-z0-9_]+)+$")
    }
}
