package com.vertyll.veds.iam.domain.model.enums

enum class SagaStepNames(
    val value: String,
) {
    CREATE_USER("CreateUser"),
    CREATE_USER_EVENT("CreateUserEvent"),
    CREATE_VERIFICATION_TOKEN("CreateVerificationToken"),
    CREATE_MAIL_EVENT("CreateMailEvent"),
    CREATE_RESET_TOKEN("CreateResetToken"),
    VERIFY_CURRENT_PASSWORD("VerifyCurrentPassword"),
    UPDATE_PASSWORD("UpdatePassword"),
    UPDATE_EMAIL("UpdateEmail"),
    ;

    companion object {
        const val COMPENSATION_PREFIX = "Compensate"

        fun fromString(value: String): SagaStepNames? = SagaStepNames.entries.find { it.value == value }

        fun compensationName(step: SagaStepNames): String = "$COMPENSATION_PREFIX${step.value}"

        fun compensationNameFromString(stepName: String): String = "$COMPENSATION_PREFIX$stepName"
    }
}
