package com.vertyll.veds.iam.domain.model.enums

enum class SagaTypes(
    val value: String,
) {
    USER_REGISTRATION("UserRegistration"),

    EMAIL_CHANGE("EmailChange"),
    EMAIL_VERIFICATION("EmailVerification"),

    PASSWORD_CHANGE("PasswordChange"),
    PASSWORD_RESET("PasswordReset"),
    ;

    companion object {
        fun fromString(value: String): SagaTypes? = SagaTypes.entries.find { it.value == value }
    }
}
