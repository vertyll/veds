package com.vertyll.veds.mail.domain.model

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
