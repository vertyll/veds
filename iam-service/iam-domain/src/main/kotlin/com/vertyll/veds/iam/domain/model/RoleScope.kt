package com.vertyll.veds.iam.domain.model

enum class RoleScope {
    GLOBAL,
    PROJECT,
    ;

    companion object {
        fun fromString(value: String): RoleScope? = entries.find { it.name == value }
    }
}
