package com.vertyll.veds.template.domain.model

object VersionGuard {
    fun requireMatch(
        currentVersion: Long?,
        expectedVersion: Long?,
        onMismatch: () -> Throwable,
    ) {
        if (expectedVersion != null && currentVersion != expectedVersion) {
            throw onMismatch()
        }
    }
}
