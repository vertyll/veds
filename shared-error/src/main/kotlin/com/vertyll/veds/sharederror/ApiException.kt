package com.vertyll.veds.sharederror

/**
 * Refuses a request, naming the error from the service's own catalogue.
 *
 * Carries no HTTP status: the status follows from [ErrorKind], and the layer that
 * throws this has no business knowing about HTTP.
 *
 * @property error the entry of the catalogue this refusal names.
 * @property params values the translated message interpolates, for example the
 *           identifier that was not found.
 * @param cause what this exception translates, when it translates one.
 */
class ApiException(
    val error: DomainError,
    val params: Map<String, Any> = emptyMap(),
    cause: Throwable? = null,
) : RuntimeException(error.key, cause)
