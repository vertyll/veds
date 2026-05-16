package com.vertyll.veds.template.application.saga.model

/**
 * Defines compensation actions for template-service sagas.
 *
 * Replace these placeholder values with your actual compensation actions
 * when cloning this service for a new microservice.
 */
enum class SagaCompensationActions(
    val value: String,
) {
    DELETE_TEMPLATE("DELETE_TEMPLATE"),
    LOG_TEMPLATE_COMPENSATION("LOG_TEMPLATE_COMPENSATION"),
    ;

    companion object {
        fun fromString(value: String): SagaCompensationActions? = SagaCompensationActions.entries.find { it.value == value }
    }
}
