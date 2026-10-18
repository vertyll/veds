package com.vertyll.veds.translation.application.port.inbound.query

import com.vertyll.veds.translation.application.dto.ExportRowResponse
import com.vertyll.veds.translation.domain.model.LanguageTag

interface TranslationExportUseCase {
    fun exportRows(): List<ExportRowResponse>

    fun exportHeaders(language: LanguageTag): List<String>
}
