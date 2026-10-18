package com.vertyll.veds.task.domain.service

import com.vertyll.veds.task.domain.error.TaskError

sealed interface AccessDecision {
    data object Permit : AccessDecision

    data class Deny(
        val reason: TaskError,
    ) : AccessDecision

    val isPermitted: Boolean
        get() = this is Permit
}
