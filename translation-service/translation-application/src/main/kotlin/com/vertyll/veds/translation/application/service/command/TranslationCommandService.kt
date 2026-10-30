package com.vertyll.veds.translation.application.service.command

import com.vertyll.veds.sharederror.ApiException
import com.vertyll.veds.sharedtranslation.IcuPatternValidator
import com.vertyll.veds.translation.application.command.ClearOverrideCommand
import com.vertyll.veds.translation.application.command.ImportTranslationsCommand
import com.vertyll.veds.translation.application.command.OverrideTranslationCommand
import com.vertyll.veds.translation.application.command.RegisterCatalogueCommand
import com.vertyll.veds.translation.application.dto.ImportReportResponse
import com.vertyll.veds.translation.application.dto.MissingTranslationResponse
import com.vertyll.veds.translation.application.dto.RejectedPatternResponse
import com.vertyll.veds.translation.application.dto.TranslationValueResponse
import com.vertyll.veds.translation.application.mapper.TranslationValueMapper
import com.vertyll.veds.translation.application.port.inbound.command.TranslationCommandUseCase
import com.vertyll.veds.translation.application.port.outbound.UseCaseLogger
import com.vertyll.veds.translation.domain.error.TranslationError
import com.vertyll.veds.translation.domain.model.LanguageTag
import com.vertyll.veds.translation.domain.model.TranslationKey
import com.vertyll.veds.translation.domain.model.TranslationValue
import com.vertyll.veds.translation.domain.model.VersionGuard
import com.vertyll.veds.translation.domain.repository.LanguageRepository
import com.vertyll.veds.translation.domain.repository.TranslationKeyRepository
import com.vertyll.veds.translation.domain.repository.TranslationValueRepository
import java.util.UUID

private const val LANGUAGE_PARAM = "language"

@Suppress("LongParameterList")
class TranslationCommandService(
    private val keyRepository: TranslationKeyRepository,
    private val valueRepository: TranslationValueRepository,
    private val languageRepository: LanguageRepository,
    private val logger: UseCaseLogger,
) : TranslationCommandUseCase {
    override fun registerCatalogue(command: RegisterCatalogueCommand): Int {
        val known = languageRepository.findAll().map { it.tag }.toSet()
        var applied = 0

        command.entries.forEach { entry ->
            val existing = keyRepository.findByKey(entry.key)

            val key =
                if (existing == null) {
                    TranslationKey(
                        key = entry.key,
                        sourceService = command.sourceService,
                        description = entry.description,
                    )
                } else if (existing.sourceService != command.sourceService) {
                    throw ApiException(
                        TranslationError.KEY_OWNED_BY_ANOTHER_SERVICE,
                        mapOf("key" to entry.key, "owner" to existing.sourceService),
                    )
                } else {
                    existing.redeclaredBy(service = command.sourceService, newDescription = entry.description)
                }
            keyRepository.save(key)

            entry.defaultValues.forEach { (rawTag, value) ->
                val tag = LanguageTag.of(rawTag)
                if (tag !in known) {
                    logger.warn("Ignoring default for unknown language {} on key {}", tag, entry.key)
                    return@forEach
                }

                val current = valueRepository.find(entry.key, tag)
                val updated =
                    current?.withSeededDefault(value)
                        ?: TranslationValue.seeded(entry.key, tag, value)

                if (current !== updated) {
                    valueRepository.save(updated)
                    applied++
                }
            }
        }

        logger.info("Registered {} keys from {}, {} values written", command.entries.size, command.sourceService, applied)
        return applied
    }

    override fun override(
        command: OverrideTranslationCommand,
        editor: UUID,
        version: Long?,
    ): TranslationValueResponse {
        val tag = requireKnownLanguage(command.language)
        requireExistingKey(command.key)
        validatePattern(command.key, tag, command.value)

        val current =
            valueRepository.find(command.key, tag)
                ?: TranslationValue(key = command.key, language = tag)

        requireSameArguments(key = command.key, tag = tag, defaultValue = current.defaultValue, newValue = command.value)

        VersionGuard.requireMatch(currentVersion = current.version, expectedVersion = version) {
            ApiException(TranslationError.VERSION_MISMATCH)
        }

        val saved = valueRepository.save(current.overriddenBy(editor, command.value))
        return TranslationValueMapper.toResponse(saved)
    }

    override fun clearOverride(
        command: ClearOverrideCommand,
        editor: UUID,
    ): TranslationValueResponse {
        val tag = requireKnownLanguage(command.language)
        val current =
            valueRepository.find(command.key, tag)
                ?: throw ApiException(
                    TranslationError.VALUE_NOT_FOUND,
                    mapOf("key" to command.key, LANGUAGE_PARAM to command.language),
                )

        return TranslationValueMapper.toResponse(valueRepository.save(current.overrideCleared(editor)))
    }

    override fun import(command: ImportTranslationsCommand): ImportReportResponse {
        val knownLanguages = languageRepository.findAll().map { it.tag }.toSet()
        val knownKeys = keyRepository.findAll().map { it.key }.toSet()

        val unknownKeys = mutableSetOf<String>()
        val unknownLanguages = mutableSetOf<String>()
        val rejected = mutableListOf<RejectedPatternResponse>()
        var applied = 0

        command.entries.forEach { row ->
            if (row.key !in knownKeys) {
                unknownKeys += row.key
                return@forEach
            }
            val tag = LanguageTag.parse(row.language)
            if (tag == null || tag !in knownLanguages) {
                unknownLanguages += row.language
                return@forEach
            }
            val failure = IcuPatternValidator.validate(language = tag.value, pattern = row.value)
            if (failure != null) {
                rejected += RejectedPatternResponse(key = row.key, language = tag.value, reason = failure)
                return@forEach
            }

            val current = valueRepository.find(row.key, tag) ?: TranslationValue(key = row.key, language = tag)
            val argumentDrift = argumentDrift(tag = tag, defaultValue = current.defaultValue, newValue = row.value)
            if (argumentDrift != null) {
                rejected += RejectedPatternResponse(key = row.key, language = tag.value, reason = argumentDrift)
                return@forEach
            }
            valueRepository.save(current.overriddenBy(command.importedBy, row.value))
            applied++
        }

        logger.info(
            "Import applied {} rows, skipped {} unknown keys, {} unknown languages, {} bad patterns",
            applied,
            unknownKeys.size,
            unknownLanguages.size,
            rejected.size,
        )

        return ImportReportResponse(
            applied = applied,
            skippedUnknownKeys = unknownKeys.sorted(),
            skippedUnknownLanguages = unknownLanguages.sorted(),
            rejectedPatterns = rejected,
            missingAfterImport = missingAfterImport(knownKeys, knownLanguages),
        )
    }

    private fun missingAfterImport(
        knownKeys: Set<String>,
        knownLanguages: Set<LanguageTag>,
    ): List<MissingTranslationResponse> =
        knownKeys
            .sorted()
            .flatMap { key ->
                knownLanguages
                    .asSequence()
                    .sortedBy { it.value }
                    .filter { valueRepository.find(key, it)?.effectiveValue.isNullOrBlank() }
                    .map { MissingTranslationResponse(key = key, language = it.value) }
                    .toList()
            }

    private fun requireKnownLanguage(raw: String): LanguageTag {
        val tag =
            LanguageTag.parse(raw)
                ?: throw ApiException(TranslationError.LANGUAGE_NOT_FOUND, mapOf(LANGUAGE_PARAM to raw))
        languageRepository.findByTag(tag)
            ?: throw ApiException(TranslationError.LANGUAGE_NOT_FOUND, mapOf(LANGUAGE_PARAM to raw))
        return tag
    }

    private fun requireExistingKey(key: String): TranslationKey =
        keyRepository.findByKey(key)
            ?: throw ApiException(TranslationError.KEY_NOT_FOUND, mapOf("key" to key))

    private fun argumentDrift(
        tag: LanguageTag,
        defaultValue: String?,
        newValue: String,
    ): String? {
        val expected = defaultValue?.let { IcuPatternValidator.argumentsOf(language = tag.value, pattern = it) } ?: return null
        val actual = IcuPatternValidator.argumentsOf(language = tag.value, pattern = newValue)
        if (expected == actual) return null

        val missing = expected - actual
        val unexpected = actual - expected
        return listOfNotNull(
            missing.takeIf { it.isNotEmpty() }?.let { "missing " + it.sorted().joinToString(", ") },
            unexpected.takeIf { it.isNotEmpty() }?.let { "unexpected " + it.sorted().joinToString(", ") },
        ).joinToString("; ")
    }

    private fun requireSameArguments(
        key: String,
        tag: LanguageTag,
        defaultValue: String?,
        newValue: String,
    ) {
        val drift = argumentDrift(tag, defaultValue, newValue) ?: return
        throw ApiException(
            TranslationError.UNKNOWN_ARGUMENTS,
            mapOf("key" to key, LANGUAGE_PARAM to tag.value, "reason" to drift),
        )
    }

    private fun validatePattern(
        key: String,
        tag: LanguageTag,
        value: String,
    ) {
        val failure = IcuPatternValidator.validate(language = tag.value, pattern = value)
        if (failure != null) {
            throw ApiException(
                TranslationError.INVALID_ICU_PATTERN,
                mapOf("key" to key, LANGUAGE_PARAM to tag.value, "reason" to failure),
            )
        }
    }
}
