package com.vertyll.veds.notification.infrastructure.websocket

import java.security.Principal

internal data class StompPrincipal(
    private val userName: String,
) : Principal {
    override fun getName(): String = userName
}
