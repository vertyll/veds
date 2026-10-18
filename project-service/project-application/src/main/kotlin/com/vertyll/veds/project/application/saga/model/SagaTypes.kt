package com.vertyll.veds.project.application.saga.model

import com.vertyll.veds.shared.saga.SagaTypeValue

enum class SagaTypes(
    override val value: String,
) : SagaTypeValue {
    PROJECT_INVITATION("ProjectInvitation"),
    PROJECT_ARCHIVAL("ProjectArchival"),
}
