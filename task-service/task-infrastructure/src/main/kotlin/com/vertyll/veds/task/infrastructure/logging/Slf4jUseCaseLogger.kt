package com.vertyll.veds.task.infrastructure.logging

import com.vertyll.veds.task.application.port.outbound.UseCaseLogger
import org.slf4j.LoggerFactory

internal class Slf4jUseCaseLogger(
    owner: Class<*>,
) : UseCaseLogger {
    private val delegate = LoggerFactory.getLogger(owner)

    override fun debug(
        message: String,
        vararg args: Any?,
    ) = delegate.debug(message, *args)

    override fun info(
        message: String,
        vararg args: Any?,
    ) = delegate.info(message, *args)

    override fun warn(
        message: String,
        vararg args: Any?,
    ) = delegate.warn(message, *args)

    override fun error(
        message: String,
        vararg args: Any?,
    ) = delegate.error(message, *args)
}
