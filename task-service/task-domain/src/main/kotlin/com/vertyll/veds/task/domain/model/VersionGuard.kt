package com.vertyll.veds.task.domain.model

object VersionGuard {
    // A null expected version asks for no check, which is why it is nullable.
    @Suppress("CanBeNonNullable")
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
