package com.vertyll.veds.shared.web.security

/**
 * The resource is the caller's own, so there is nothing to refuse.
 *
 * Reading your notifications or editing your profile needs no permission: the caller's id from the
 * token narrows the query, and a row belonging to somebody else is never a candidate. This is the
 * one kind of endpoint where "no guard" is the right answer, which is exactly why it has to be
 * stated rather than inferred from an absence.
 *
 * @property how what narrows the work to the caller.
 */
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class ScopedToCaller(
    val how: String,
)
