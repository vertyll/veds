package com.vertyll.veds.translation.application.command

import java.util.UUID

data class ImportTranslationsCommand(
    val entries: List<ImportedTranslationCommand>,
    val importedBy: UUID,
)
