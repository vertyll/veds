package com.vertyll.veds.iam.domain.model

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
