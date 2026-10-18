package com.vertyll.veds.project.application.port.outbound

import com.vertyll.veds.project.domain.model.LanguageTag

fun interface SupportedLanguagesPort {
    fun supported(): Set<LanguageTag>
}
