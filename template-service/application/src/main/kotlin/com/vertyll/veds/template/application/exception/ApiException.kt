package com.vertyll.veds.template.application.exception

import org.springframework.http.HttpStatus

class ApiException(
    message: String,
    val status: HttpStatus,
) : RuntimeException(message)
