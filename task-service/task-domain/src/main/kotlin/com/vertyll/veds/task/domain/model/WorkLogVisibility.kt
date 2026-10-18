package com.vertyll.veds.task.domain.model

enum class WorkLogVisibility {
    ALL,
    HIDDEN,
    VISIBLE,
    ;

    companion object {
        fun fromString(value: String): WorkLogVisibility? = entries.find { it.name == value.uppercase() }
    }
}
