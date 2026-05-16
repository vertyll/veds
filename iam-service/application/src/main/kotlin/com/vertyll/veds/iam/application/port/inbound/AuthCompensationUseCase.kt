package com.vertyll.veds.iam.application.port.inbound

@Suppress("kotlin:S6517")
interface AuthCompensationUseCase {
    fun compensate(
        action: String,
        event: Map<String, Any?>,
    )
}
