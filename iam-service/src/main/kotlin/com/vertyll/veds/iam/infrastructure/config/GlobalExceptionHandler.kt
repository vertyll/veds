package com.vertyll.veds.iam.infrastructure.config

import com.vertyll.veds.iam.infrastructure.exception.ApiException
import com.vertyll.veds.iam.infrastructure.response.ApiResponse
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.orm.ObjectOptimisticLockingFailureException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice(basePackages = ["com.vertyll.veds.identity"])
class GlobalExceptionHandler {
    private val logger = LoggerFactory.getLogger(GlobalExceptionHandler::class.java)

    private companion object {
        private const val INVALID_VALUE = "Invalid value"
        private const val VALIDATION_FAILED = "Validation failed"
        private const val AN_UNEXPECTED_ERROR_OCCURRED = "An unexpected error occurred"
        private const val OPTIMISTIC_LOCKING_FAILURE = "Data has been modified by another transaction. Please refresh and try again."
    }

    @ExceptionHandler(ApiException::class)
    fun handleApiException(ex: ApiException): ResponseEntity<ApiResponse<Any>> {
        logger.error("API Exception: {}", ex.message)
        return ApiResponse.buildResponse(
            data = null,
            message = ex.message,
            status = ex.status,
        )
    }

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidationExceptions(ex: MethodArgumentNotValidException): ResponseEntity<ApiResponse<Map<String, String>>> {
        logger.error("Validation Exception: {}", ex.message)

        val errors =
            ex.bindingResult.fieldErrors.associate { error ->
                error.field to (error.defaultMessage ?: INVALID_VALUE)
            }

        return ApiResponse.buildResponse(
            data = errors,
            message = VALIDATION_FAILED,
            status = HttpStatus.BAD_REQUEST,
        )
    }

    @ExceptionHandler(ObjectOptimisticLockingFailureException::class)
    fun handleOptimisticLockingFailure(ex: ObjectOptimisticLockingFailureException): ResponseEntity<ApiResponse<Any>> {
        logger.error("Optimistic Locking Exception: {}", ex.message)
        return ApiResponse.buildResponse(
            data = null,
            message = OPTIMISTIC_LOCKING_FAILURE,
            status = HttpStatus.CONFLICT,
        )
    }

    @ExceptionHandler(Exception::class)
    fun handleGenericException(ex: Exception): ResponseEntity<ApiResponse<Any>> {
        logger.error("Unhandled exception", ex)
        return ApiResponse.buildResponse(
            data = null,
            message = AN_UNEXPECTED_ERROR_OCCURRED,
            status = HttpStatus.INTERNAL_SERVER_ERROR,
        )
    }
}
