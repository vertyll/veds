package com.vertyll.veds.iam.domain.model.enums

import com.vertyll.veds.sharedinfrastructure.saga.contract.SagaTypeValue

enum class SagaTypes(
    override val value: String,
) : SagaTypeValue {
    USER_REGISTRATION("UserRegistration"),

    EMAIL_CHANGE("EmailChange"),
    EMAIL_VERIFICATION("EmailVerification"),

    PASSWORD_CHANGE("PasswordChange"),
    PASSWORD_RESET("PasswordReset"),
    ;

    companion object {
        fun fromString(value: String): SagaTypes? = entries.find { it.value == value }
    }
}
