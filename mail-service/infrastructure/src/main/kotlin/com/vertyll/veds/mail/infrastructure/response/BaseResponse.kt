package com.vertyll.veds.mail.infrastructure.response

import java.time.LocalDateTime

internal abstract class BaseResponse<T>(
    open val data: T?,
    open val message: String,
    open val timestamp: LocalDateTime = LocalDateTime.now(),
)
