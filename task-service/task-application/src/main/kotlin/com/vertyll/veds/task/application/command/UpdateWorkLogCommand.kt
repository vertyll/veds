package com.vertyll.veds.task.application.command

import java.time.LocalDate

data class UpdateWorkLogCommand(
    val minutes: Int,
    val workedOn: LocalDate,
    val description: String? = null,
    val hidden: Boolean = false,
)
