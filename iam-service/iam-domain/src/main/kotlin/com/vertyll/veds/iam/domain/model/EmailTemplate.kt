package com.vertyll.veds.iam.domain.model

enum class EmailTemplate(
    val templateName: String,
) {
    ACTIVATE_ACCOUNT("ACTIVATE_ACCOUNT"),
    WELCOME_EMAIL("WELCOME_EMAIL"),

    RESET_PASSWORD("RESET_PASSWORD"),
    CHANGE_PASSWORD("CHANGE_PASSWORD"),
    SET_NEW_PASSWORD("SET_NEW_PASSWORD"),

    CHANGE_EMAIL("CHANGE_EMAIL"),
}
