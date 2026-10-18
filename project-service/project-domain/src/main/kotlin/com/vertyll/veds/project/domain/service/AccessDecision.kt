package com.vertyll.veds.project.domain.service

import com.vertyll.veds.project.domain.error.ProjectError

sealed interface AccessDecision {
    data object Permit : AccessDecision

    data class Deny(
        val reason: ProjectError,
    ) : AccessDecision

    val isPermitted: Boolean
        get() = this is Permit
}
