package com.vertyll.veds.translation.infrastructure.config

import com.vertyll.veds.translation.domain.model.Language
import com.vertyll.veds.translation.domain.model.LanguageTag
import com.vertyll.veds.translation.domain.repository.LanguageRepository
import org.slf4j.LoggerFactory
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
@Order(1)
internal class LanguageSeeder(
    private val languageRepository: LanguageRepository,
) : ApplicationRunner {
    private companion object {
        private val logger = LoggerFactory.getLogger(LanguageSeeder::class.java)

        private val SEEDED =
            listOf(
                Language(tag = LanguageTag.of("pl"), displayName = "Polski", isDefault = true),
                Language(tag = LanguageTag.of("en"), displayName = "English"),
            )
    }

    @Transactional
    override fun run(args: ApplicationArguments) {
        SEEDED.forEach { language ->
            if (languageRepository.findByTag(language.tag) != null) {
                logger.debug("Language already present, skipping: {}", language.tag)
                return@forEach
            }
            languageRepository.save(language)
            logger.info("Seeded language: {} ({})", language.tag, language.displayName)
        }
    }
}
