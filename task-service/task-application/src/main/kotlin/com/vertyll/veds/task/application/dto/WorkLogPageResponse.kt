package com.vertyll.veds.task.application.dto

data class WorkLogPageResponse(
    val content: List<WorkLogEntryResponse>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalMinutes: Int,
)
