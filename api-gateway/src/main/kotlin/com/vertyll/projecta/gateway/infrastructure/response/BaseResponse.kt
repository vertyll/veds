package com.vertyll.projecta.gateway.infrastructure.response

import java.time.LocalDateTime

abstract class BaseResponse<T>(
    open val data: T?,
    open val message: String,
    open val timestamp: LocalDateTime = LocalDateTime.now(),
)
