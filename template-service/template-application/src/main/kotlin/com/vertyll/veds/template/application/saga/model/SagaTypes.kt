package com.vertyll.veds.template.application.saga.model

import com.vertyll.veds.shared.saga.SagaTypeValue

enum class SagaTypes(
    override val value: String,
) : SagaTypeValue {
    TEMPLATE_PROCESSING("TemplateProcessing"),
}
