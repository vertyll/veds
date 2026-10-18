@file:OptIn(ExperimentalUuidApi::class)

package com.vertyll.veds.translation.domain.model

import java.time.Instant
import java.util.UUID
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlin.uuid.toJavaUuid

data class TranslationValue(
    val id: UUID = Uuid.generateV7().toJavaUuid(),
    val key: String,
    val language: LanguageTag,
    val defaultValue: String? = null,
    val overrideValue: String? = null,
    val updatedBy: UUID? = null,
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now(),
    val version: Long? = null,
) {
    val effectiveValue: String?
        get() = overrideValue ?: defaultValue

    val isOverridden: Boolean
        get() = overrideValue != null

    fun withSeededDefault(value: String): TranslationValue =
        if (defaultValue == value) this else copy(defaultValue = value, updatedAt = Instant.now())

    fun overriddenBy(
        editor: UUID,
        value: String,
    ): TranslationValue {
        require(value.isNotBlank()) { "translation value must not be blank" }
        return copy(overrideValue = value, updatedBy = editor, updatedAt = Instant.now())
    }

    fun overrideCleared(editor: UUID): TranslationValue = copy(overrideValue = null, updatedBy = editor, updatedAt = Instant.now())

    companion object {
        fun seeded(
            key: String,
            language: LanguageTag,
            value: String,
        ): TranslationValue = TranslationValue(key = key, language = language, defaultValue = value)
    }
}
