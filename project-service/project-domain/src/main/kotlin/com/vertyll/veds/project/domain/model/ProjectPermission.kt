package com.vertyll.veds.project.domain.model

enum class ProjectPermission {
    VIEW_PROJECT,
    EDIT_PROJECT,
    DELETE_PROJECT,
    INVITE_USERS,
    MANAGE_MEMBERS,
    ;

    companion object {
        fun of(name: String): ProjectPermission? = entries.find { it.name == name }
    }
}
