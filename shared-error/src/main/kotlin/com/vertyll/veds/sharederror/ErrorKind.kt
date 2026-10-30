package com.vertyll.veds.sharederror

/**
 * What kind of failure an error describes, in terms every context shares.
 *
 * This is transport vocabulary, not domain knowledge: it exists so one place can
 * decide which HTTP status a failure becomes, instead of each service repeating
 * the same mapping. What a failure *means* stays in the service's own error
 * catalogue, which names the key.
 */
enum class ErrorKind {
    /** What was asked for does not exist. */
    NOT_FOUND,

    /** The caller did not prove who they are. */
    UNAUTHENTICATED,

    /** The caller may not do this. */
    ACCESS_DENIED,

    /** The request clashes with the current state. */
    CONFLICT,

    /** The request itself is malformed or breaks a rule. */
    INVALID,

    /** A condition the caller set, such as `If-Match`, does not hold. */
    PRECONDITION_FAILED,

    /** What was asked for existed, and no longer does. */
    GONE,

    /** The service is set up wrongly; the caller can do nothing about it. */
    MISCONFIGURED,
}
