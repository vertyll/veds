package com.vertyll.veds.iam.application.saga.model

enum class SagaCompensationActions(
    val value: String,
) {
    DELETE_USER("DELETE_USER"),
    DELETE_VERIFICATION_TOKEN("DELETE_VERIFICATION_TOKEN"),
    REVERT_PASSWORD_UPDATE("REVERT_PASSWORD_UPDATE"),
    REVERT_EMAIL_UPDATE("REVERT_EMAIL_UPDATE"),
    ;

    companion object {
        fun fromString(value: String): SagaCompensationActions? = entries.find { it.value == value }
    }
}
