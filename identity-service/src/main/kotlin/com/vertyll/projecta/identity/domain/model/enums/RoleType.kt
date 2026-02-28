package com.vertyll.projecta.identity.domain.model.enums

enum class RoleType(
    val value: String,
) {
    USER("USER"),
    ADMIN("ADMIN"),
    ;

    companion object {
        fun fromString(name: String): RoleType? = RoleType.entries.find { it.value == name }
    }
}
