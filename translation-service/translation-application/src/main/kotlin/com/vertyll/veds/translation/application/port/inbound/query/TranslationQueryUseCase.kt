package com.vertyll.veds.translation.application.port.inbound.query

import com.vertyll.veds.translation.application.dto.LanguageResponse
import com.vertyll.veds.translation.application.dto.PagedResponse
import com.vertyll.veds.translation.application.dto.TranslationKeyDetailsResponse
import com.vertyll.veds.translation.application.dto.TranslationSnapshotResponse

interface TranslationQueryUseCase {
    fun snapshot(language: String): TranslationSnapshotResponse

    fun languages(): List<LanguageResponse>

    fun searchKeys(
        searchTerm: String?,
        sourceService: String?,
        onlyMissing: Boolean,
        page: Int,
        size: Int,
    ): PagedResponse<TranslationKeyDetailsResponse>

    fun keyDetails(key: String): TranslationKeyDetailsResponse
}
