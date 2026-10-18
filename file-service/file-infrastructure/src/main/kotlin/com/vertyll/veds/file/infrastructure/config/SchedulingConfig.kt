package com.vertyll.veds.file.infrastructure.config

import com.vertyll.veds.file.application.port.inbound.command.FileCommandUseCase
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.scheduling.annotation.Scheduled

@Configuration
@EnableScheduling
internal class SchedulingConfig(
    private val fileCommands: FileCommandUseCase,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    @Scheduled(cron = "0 10 * * * ?")
    fun purgeAbandonedUploads() {
        val purged = fileCommands.purgeAbandonedUploads()
        logger.debug("Abandoned-upload sweep removed {} records", purged)
    }

    @Scheduled(cron = "0 25 * * * ?")
    fun purgeDeletedObjects() {
        val purged = fileCommands.purgeDeletedObjects()
        logger.debug("Deleted-object sweep removed {} objects", purged)
    }
}
