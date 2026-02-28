package com.vertyll.veds.iam.domain.model.enums

enum class EmailTemplate(
    val templateName: String,
) {
    // User registration and account management
    ACTIVATE_ACCOUNT("ACTIVATE_ACCOUNT"),
    WELCOME_EMAIL("WELCOME_EMAIL"),

    // Password management
    RESET_PASSWORD("RESET_PASSWORD"),
    CHANGE_PASSWORD("CHANGE_PASSWORD"),
    SET_NEW_PASSWORD("SET_NEW_PASSWORD"),

    // Email management
    CHANGE_EMAIL("CHANGE_EMAIL"),
    ;

    companion object {
        fun fromTemplateName(name: String): EmailTemplate? = EmailTemplate.entries.find { it.templateName == name }
    }
}
