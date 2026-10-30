package com.vertyll.veds.shared.web.error

import com.vertyll.veds.sharederror.ErrorKind
import org.springframework.http.HttpStatus

/**
 * The single place a failure becomes a status code.
 *
 * Kept away from the services so two of them cannot disagree about what, say, a
 * precondition failure looks like on the wire.
 */
object ErrorHttpStatusMapper {
    /** The HTTP status [kind] becomes. */
    fun toStatus(kind: ErrorKind): HttpStatus =
        when (kind) {
            ErrorKind.NOT_FOUND -> HttpStatus.NOT_FOUND
            ErrorKind.UNAUTHENTICATED -> HttpStatus.UNAUTHORIZED
            ErrorKind.ACCESS_DENIED -> HttpStatus.FORBIDDEN
            ErrorKind.CONFLICT -> HttpStatus.CONFLICT
            ErrorKind.INVALID -> HttpStatus.BAD_REQUEST
            ErrorKind.PRECONDITION_FAILED -> HttpStatus.PRECONDITION_FAILED
            ErrorKind.GONE -> HttpStatus.GONE
            ErrorKind.MISCONFIGURED -> HttpStatus.INTERNAL_SERVER_ERROR
        }
}
