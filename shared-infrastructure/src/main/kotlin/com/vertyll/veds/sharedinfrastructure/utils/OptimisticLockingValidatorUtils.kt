package com.vertyll.veds.sharedinfrastructure.utils

import org.springframework.dao.OptimisticLockingFailureException
import java.util.Objects

/**
 * Helpers for verifying that a client-supplied expected version matches
 * the version currently persisted, enabling **optimistic concurrency** on
 * the application side before any write hits the database.
 *
 * Typical use is to pair these helpers with [ETagUtils]: the controller
 * parses `If-Match` into an `expectedVersion`, then the application
 * service calls [validate] before performing the mutation.
 */
object OptimisticLockingValidatorUtils {
    /**
     * Throws [OptimisticLockingFailureException] when [expectedVersion] is
     * non-null and does not match [currentVersion]. A `null`
     * [expectedVersion] is treated as "no check requested" and silently
     * passes.
     */
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

    /**
     * Same contract as [validate] but throws a caller-supplied exception
     * (built lazily by [exceptionSupplier]), letting domain modules surface
     * a domain-specific error type instead of the generic Spring one.
     */
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
