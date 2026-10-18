package com.vertyll.veds.mail.application.port.outbound

interface UseCaseLogger {
    fun debug(
        message: String,
        vararg args: Any?,
    )

    fun info(
        message: String,
        vararg args: Any?,
    )

    fun warn(
        message: String,
        vararg args: Any?,
    )

    fun error(
        message: String,
        vararg args: Any?,
    )
}
