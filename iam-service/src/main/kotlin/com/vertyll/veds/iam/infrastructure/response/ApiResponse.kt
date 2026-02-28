package com.vertyll.veds.iam.infrastructure.response

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import java.time.LocalDateTime

class ApiResponse<T> private constructor(
    data: T?,
    message: String,
    timestamp: LocalDateTime,
) : BaseResponse<T>(data, message, timestamp) {
    companion object {
        /**
         * Creates a response entity with an ApiResponse body.
         *
         * @param data The data to include in the response can be null
         * @param message The message to include in the response, defaults to empty string if null
         * @param status The HTTP status code for the response
         * @return A ResponseEntity with an ApiResponse body
         */
        fun <T> buildResponse(
            data: T?,
            message: String?,
            status: HttpStatus,
        ): ResponseEntity<ApiResponse<T>> {
            val response =
                ApiResponse(
                    data = data,
                    message = message ?: "",
                    timestamp = LocalDateTime.now(),
                )

            return ResponseEntity(response, status)
        }
    }
}
