package com.vertyll.veds.shared.web.error

import com.vertyll.veds.sharederror.ApiException
import com.vertyll.veds.sharederror.DomainError
import com.vertyll.veds.sharederror.ErrorKind
import org.springframework.core.MethodParameter
import org.springframework.http.HttpStatus
import org.springframework.http.ProblemDetail
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.mock.http.MockHttpInputMessage
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.security.authorization.AuthorizationDecision
import org.springframework.security.authorization.AuthorizationDeniedException
import org.springframework.validation.BeanPropertyBindingResult
import org.springframework.validation.FieldError
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.context.request.ServletWebRequest
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException
import org.springframework.web.server.ResponseStatusException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class GlobalExceptionHandlerTest {
    private enum class TaskError(
        override val key: String,
        override val kind: ErrorKind,
    ) : DomainError {
        NOT_FOUND("task.not_found", ErrorKind.NOT_FOUND),
        MISCONFIGURED("task.misconfigured", ErrorKind.MISCONFIGURED),
    }

    private val handler = GlobalExceptionHandler()
    private val request = ServletWebRequest(MockHttpServletRequest("GET", "/tasks/42"))
    private val parameter = MethodParameter(String::class.java.getMethod("concat", String::class.java), 0)

    private fun ProblemDetail.code() = properties?.get("code")

    @Test
    fun `a refusal answers with its kind, its key and its params`() {
        val problem = handler.handleApiException(ApiException(TaskError.NOT_FOUND, mapOf("id" to "42")), request)

        assertEquals(404, problem.status)
        assertEquals("task.not_found", problem.code())
        assertEquals(mapOf("id" to "42"), problem.properties?.get("params"))
        assertEquals("/tasks/42", problem.instance.toString())
    }

    @Test
    fun `a misconfiguration is a server failure without params`() {
        val problem = handler.handleApiException(ApiException(TaskError.MISCONFIGURED), request)

        assertEquals(500, problem.status)
        assertNull(problem.properties?.get("params"))
    }

    @Test
    fun `a refused authorization answers forbidden`() {
        val problem =
            handler.handleAccessDenied(AuthorizationDeniedException("denied", AuthorizationDecision(false)), request)

        assertEquals(403, problem.status)
        assertEquals("common.access_denied", problem.code())
    }

    @Test
    fun `a failed validation names the fields at fault`() {
        val errors = BeanPropertyBindingResult(Any(), "request")
        errors.addError(FieldError("request", "name", "must not be blank"))
        errors.addError(FieldError("request", "payload", null, false, null, null, null))

        val problem = handler.handleValidationExceptions(MethodArgumentNotValidException(parameter, errors), request)

        assertEquals(400, problem.status)
        assertEquals("common.validation_failed", problem.code())
        assertEquals(
            mapOf("name" to "must not be blank", "payload" to "common.invalid_value"),
            problem.properties?.get("fields"),
        )
    }

    @Test
    fun `a parameter of the wrong type is named as invalid`() {
        val mismatch = MethodArgumentTypeMismatchException("abc", Int::class.java, "page", parameter, null)

        val problem = handler.handleTypeMismatch(mismatch, request)

        assertEquals(400, problem.status)
        assertEquals(mapOf("page" to "common.invalid_value"), problem.properties?.get("fields"))
    }

    @Test
    fun `an unreadable body is a validation problem with no fields`() {
        val unreadable = HttpMessageNotReadableException("bad json", MockHttpInputMessage(ByteArray(0)))

        val problem = handler.handleUnreadableBody(unreadable, request)

        assertEquals(400, problem.status)
        assertEquals("common.validation_failed", problem.code())
        assertNull(problem.properties?.get("fields"))
    }

    @Test
    fun `a failure of the framework keeps its own status`() {
        val problem = handler.handleUnexpected(ResponseStatusException(HttpStatus.METHOD_NOT_ALLOWED), request)

        assertEquals(405, problem.status)
    }

    @Test
    fun `anything nobody recognized is a server failure that says nothing of the cause`() {
        val problem = handler.handleUnexpected(IllegalStateException("database password is hunter2"), request)

        assertEquals(500, problem.status)
        assertEquals("common.unexpected_error", problem.code())
        assertNull(problem.detail)
    }
}
