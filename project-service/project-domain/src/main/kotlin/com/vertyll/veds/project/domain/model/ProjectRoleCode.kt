package com.vertyll.veds.project.domain.model

@JvmInline
value class ProjectRoleCode(
    val value: String,
) {
    init {
        require(value.matches(PATTERN)) { "project role code '$value' must be UPPER_SNAKE_CASE" }
    }

    override fun toString(): String = value

    companion object {
        private val PATTERN = Regex("^[A-Z][A-Z0-9_]{0,31}$")

        val MANAGER = ProjectRoleCode("MANAGER")
        val MEMBER = ProjectRoleCode("MEMBER")
        val CLIENT = ProjectRoleCode("CLIENT")

        val stock: List<ProjectRoleCode> = listOf(MANAGER, MEMBER, CLIENT)

        fun of(value: String): ProjectRoleCode = ProjectRoleCode(value.trim().uppercase())
    }
}
