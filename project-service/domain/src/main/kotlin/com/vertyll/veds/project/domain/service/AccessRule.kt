package com.vertyll.veds.project.domain.service

internal fun interface AccessRule {
    fun evaluate(request: AccessRequest): AccessDecision?
}
