package com.vertyll.veds.shared.web.error

import com.vertyll.veds.sharederror.ErrorKind
import org.springframework.http.HttpStatus
import kotlin.test.Test
import kotlin.test.assertEquals

/** A kind is transport vocabulary; this is the whole of what it decides. */
class ErrorHttpStatusMapperTest {
    @Test
    fun `every kind maps to the status that says what happened`() {
        val expected =
            mapOf(
                ErrorKind.NOT_FOUND to HttpStatus.NOT_FOUND,
                ErrorKind.UNAUTHENTICATED to HttpStatus.UNAUTHORIZED,
                ErrorKind.ACCESS_DENIED to HttpStatus.FORBIDDEN,
                ErrorKind.CONFLICT to HttpStatus.CONFLICT,
                ErrorKind.INVALID to HttpStatus.BAD_REQUEST,
                ErrorKind.PRECONDITION_FAILED to HttpStatus.PRECONDITION_FAILED,
                ErrorKind.GONE to HttpStatus.GONE,
                ErrorKind.MISCONFIGURED to HttpStatus.INTERNAL_SERVER_ERROR,
            )

        assertEquals(ErrorKind.entries.toSet(), expected.keys, "a new kind needs a status, not a default")
        expected.forEach { (kind, status) -> assertEquals(status, ErrorHttpStatusMapper.toStatus(kind)) }
    }
}
