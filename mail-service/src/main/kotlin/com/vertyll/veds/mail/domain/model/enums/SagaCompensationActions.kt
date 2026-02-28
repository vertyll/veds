package com.vertyll.veds.mail.domain.model.enums

/**
 * Defines compensation actions for mail service sagas
 */
enum class SagaCompensationActions(
    val value: String,
) {
    LOG_EMAIL_COMPENSATION("LOG_EMAIL_COMPENSATION"),
    LOG_TEMPLATE_COMPENSATION("LOG_TEMPLATE_COMPENSATION"),
    DELETE_EMAIL_LOG("DELETE_EMAIL_LOG"),
    ;

    companion object {
        fun fromString(value: String): SagaCompensationActions? = SagaCompensationActions.entries.find { it.value == value }
    }
}
