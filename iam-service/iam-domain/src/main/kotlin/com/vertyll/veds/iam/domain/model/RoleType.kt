package com.vertyll.veds.iam.domain.model

enum class RoleType(
    val value: String,
) {
    USER("USER"),
    ADMIN("ADMIN"),
    ;

    companion object {
        fun fromString(name: String): RoleType? = entries.find { it.value == name }
    }
}
