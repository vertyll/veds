package com.vertyll.veds.template.application.saga.model

import com.vertyll.veds.shared.saga.SagaTypeValue

enum class SagaStepNames(
    override val value: String,
) : SagaTypeValue {
    PROCESS_TEMPLATE("ProcessTemplate"),
    PERSIST_TEMPLATE("PersistTemplate"),
    PUBLISH_TEMPLATE_EVENT("PublishTemplateEvent"),
}
