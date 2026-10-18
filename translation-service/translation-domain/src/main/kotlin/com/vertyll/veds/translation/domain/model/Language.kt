package com.vertyll.veds.translation.domain.model

import java.time.Instant

data class Language(
    val tag: LanguageTag,
    val displayName: String,
    val isDefault: Boolean = false,
    val createdAt: Instant = Instant.now(),
) {
    init {
        require(displayName.isNotBlank()) { "language display name must not be blank" }
    }
}
