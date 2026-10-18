package com.vertyll.veds.translation.application.port.inbound.command

import com.vertyll.veds.translation.application.command.ClearOverrideCommand
import com.vertyll.veds.translation.application.command.ImportTranslationsCommand
import com.vertyll.veds.translation.application.command.OverrideTranslationCommand
import com.vertyll.veds.translation.application.command.RegisterCatalogueCommand
import com.vertyll.veds.translation.application.dto.ImportReportResponse
import com.vertyll.veds.translation.application.dto.TranslationValueResponse
import java.util.UUID

interface TranslationCommandUseCase {
    fun registerCatalogue(command: RegisterCatalogueCommand): Int

    fun override(
        command: OverrideTranslationCommand,
        editor: UUID,
        version: Long? = null,
    ): TranslationValueResponse

    fun clearOverride(
        command: ClearOverrideCommand,
        editor: UUID,
    ): TranslationValueResponse

    fun import(command: ImportTranslationsCommand): ImportReportResponse
}
