package com.vertyll.veds.iam.application.port.inbound.command

import com.vertyll.veds.iam.application.dto.AuthenticatedIdentity

@Suppress("kotlin:S6517")
interface ProvisionCurrentUserUseCase {
    fun provision(identity: AuthenticatedIdentity)
}
