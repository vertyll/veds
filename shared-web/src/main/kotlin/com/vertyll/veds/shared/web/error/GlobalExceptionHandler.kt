package com.vertyll.veds.shared.web.error

import com.vertyll.veds.sharederror.ApiException
import com.vertyll.veds.sharederror.ErrorKind
import org.slf4j.LoggerFactory
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.http.HttpStatus
import org.springframework.http.ProblemDetail
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.security.authorization.AuthorizationDeniedException
import org.springframework.web.ErrorResponse
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.context.request.WebRequest
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException

/**
 * Answers every refusal as an RFC 9457 problem document, whichever service threw it.
 *
 * A caller sees one media type — `application/problem+json` — and one identity per
 * kind of failure, so the front-end handles a refusal from any service the same
 * way. What changes is the key, and that comes from the service's own catalogue.
 *
 * `type` identifies the problem; `code` repeats the bare catalogue key so a client
 * can look up its translation without taking the URI apart. `detail` is left out on
 * purpose: RFC 9457 wants prose there, and the prose belongs to
 * `translation-service`, which resolves `code` in the reader's own language.
 *
 * Spring's own failures — an unsupported method, a missing parameter — answer in
 * the same shape through `spring.mvc.problemdetails.enabled`, defaulted on in
 * `shared-web-config.yml`. A rejected body, field or parameter is handled here
 * instead, as a validation problem naming the rejected fields where there are any.
 * This advice runs first so those win over Boot's.
 *
 * Registered by [ErrorHandlingAutoConfiguration]; a service that needs to differ
 * declares its own `@RestControllerAdvice` at a higher precedence.
 */
@RestControllerAdvice
@Order(GlobalExceptionHandler.ADVICE_ORDER)
class GlobalExceptionHandler {
    private val log = LoggerFactory.getLogger(GlobalExceptionHandler::class.java)

    companion object {
        /**
         * Ahead of Boot's own problem-detail advice so the handlers here win, and
         * short of the highest precedence so a service can still put its own in
         * front of this one.
         */
        const val ADVICE_ORDER: Int = Ordered.HIGHEST_PRECEDENCE + 10

        private const val ACCESS_DENIED = "common.access_denied"
        private const val VALIDATION_FAILED = "common.validation_failed"
        private const val UNEXPECTED_ERROR = "common.unexpected_error"
        private const val INVALID_VALUE = "common.invalid_value"
    }

    @ExceptionHandler(ApiException::class)
    fun handleApiException(
        ex: ApiException,
        request: WebRequest,
    ): ProblemDetail {
        if (ex.error.kind == ErrorKind.MISCONFIGURED) {
            log.error("Misconfiguration: {} params={}", ex.error.key, ex.params, ex)
        } else {
            log.debug("Rejected request: {} params={}", ex.error.key, ex.params)
        }

        return problem(
            status = ErrorHttpStatusMapper.toStatus(ex.error.kind),
            code = ex.error.key,
            request = request,
            properties = if (ex.params.isEmpty()) emptyMap() else mapOf("params" to ex.params),
        )
    }

    @ExceptionHandler(AuthorizationDeniedException::class)
    fun handleAccessDenied(
        ex: AuthorizationDeniedException,
        request: WebRequest,
    ): ProblemDetail {
        log.debug("Access denied: {}", ex.message)

        return problem(HttpStatus.FORBIDDEN, ACCESS_DENIED, request)
    }

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidationExceptions(
        ex: MethodArgumentNotValidException,
        request: WebRequest,
    ): ProblemDetail {
        val fields = ex.bindingResult.fieldErrors.associate { it.field to (it.defaultMessage ?: INVALID_VALUE) }
        log.debug("Validation rejected: {}", fields)

        return validationProblem(fields, request)
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException::class)
    fun handleTypeMismatch(
        ex: MethodArgumentTypeMismatchException,
        request: WebRequest,
    ): ProblemDetail {
        log.debug("Unparseable request parameter '{}': {}", ex.name, ex.value)

        return validationProblem(mapOf(ex.name to INVALID_VALUE), request)
    }

    @ExceptionHandler(HttpMessageNotReadableException::class)
    fun handleUnreadableBody(
        ex: HttpMessageNotReadableException,
        request: WebRequest,
    ): ProblemDetail {
        log.debug("Unreadable request body: {}", ex.message)

        return validationProblem(emptyMap(), request)
    }

    /**
     * The last resort, and deliberately not the first.
     *
     * This advice runs ahead of Boot's so the handlers above win, which means Spring's
     * own failures arrive here too. Those already carry the status they deserve — an
     * unknown path is a `404`, not a `500` — and their document is Spring's to build,
     * so it is passed through untouched. Only what nothing recognized is a `500`.
     *
     * Such a document carries no `code`: that member names a key in a service's error
     * catalogue, and a request rejected before it reached the application has no entry
     * in one. A client reads `status` for these, which is what it means.
     */
    @ExceptionHandler(Exception::class)
    fun handleUnexpected(
        ex: Exception,
        request: WebRequest,
    ): ProblemDetail {
        if (ex is ErrorResponse) {
            log.debug("Rejected by the framework: {} {}", ex.statusCode, ex.javaClass.simpleName)
            return ex.body
        }

        log.error("Unhandled exception", ex)

        return problem(HttpStatus.INTERNAL_SERVER_ERROR, UNEXPECTED_ERROR, request)
    }

    private fun validationProblem(
        fields: Map<String, String>,
        request: WebRequest,
    ): ProblemDetail =
        problem(
            status = HttpStatus.BAD_REQUEST,
            code = VALIDATION_FAILED,
            request = request,
            properties = if (fields.isEmpty()) emptyMap() else mapOf("fields" to fields),
        )

    private fun problem(
        status: HttpStatus,
        code: String,
        request: WebRequest,
        properties: Map<String, Any> = emptyMap(),
    ): ProblemDetail =
        Problems.of(
            status = status,
            code = code,
            instance = request.getDescription(false).removePrefix("uri="),
            properties = properties,
        )
}
