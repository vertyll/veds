package com.vertyll.veds.sharedinfrastructure.util

import org.springframework.dao.OptimisticLockingFailureException
import java.util.Objects

object OptimisticLockingValidator {
    fun validate(
        currentVersion: Long?,
        expectedVersion: Long?,
    ) {
        if (expectedVersion != null && !Objects.equals(currentVersion, expectedVersion)) {
            throw OptimisticLockingFailureException(
                "Version mismatch: expected $expectedVersion but was $currentVersion",
            )
        }
    }

    fun validate(
        currentVersion: Long?,
        expectedVersion: Long?,
        exceptionSupplier: () -> Throwable,
    ) {
        if (expectedVersion != null && !Objects.equals(currentVersion, expectedVersion)) {
            throw exceptionSupplier()
        }
    }
}
