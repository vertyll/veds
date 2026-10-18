package com.vertyll.veds.translation.application.mapper

import com.vertyll.veds.translation.application.dto.TranslationValueResponse
import com.vertyll.veds.translation.domain.model.TranslationValue

internal object TranslationValueMapper {
    fun toResponse(value: TranslationValue): TranslationValueResponse =
        TranslationValueResponse(
            language = value.language.value,
            defaultValue = value.defaultValue,
            overrideValue = value.overrideValue,
            effectiveValue = value.effectiveValue,
            isOverridden = value.isOverridden,
            updatedAt = value.updatedAt,
            version = value.version,
        )
}
