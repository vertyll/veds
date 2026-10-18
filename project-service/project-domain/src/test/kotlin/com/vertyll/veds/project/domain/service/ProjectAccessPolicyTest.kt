@file:OptIn(ExperimentalUuidApi::class)

package com.vertyll.veds.project.domain.service

import com.vertyll.veds.project.domain.error.ProjectError
import com.vertyll.veds.project.domain.model.LanguageTag
import com.vertyll.veds.project.domain.model.Project
import com.vertyll.veds.project.domain.model.ProjectMember
import com.vertyll.veds.project.domain.model.ProjectPermission
import com.vertyll.veds.project.domain.model.ProjectRole
import com.vertyll.veds.project.domain.model.ProjectRoleCode
import com.vertyll.veds.project.domain.model.Translation
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlin.uuid.toJavaUuid

class ProjectAccessPolicyTest {
    private val ownerId = Uuid.generateV7().toJavaUuid()
    private val memberId = Uuid.generateV7().toJavaUuid()
    private val strangerId = Uuid.generateV7().toJavaUuid()

    private val managerRole =
        ProjectRole(
            code = ProjectRoleCode.MANAGER,
            permissions = ProjectPermission.entries.mapTo(mutableSetOf()) { it.name },
            translations = allLanguages("Manager"),
        )

    private val clientRole =
        ProjectRole(
            code = ProjectRoleCode.CLIENT,
            permissions = setOf(ProjectPermission.VIEW_PROJECT.name, "VIEW_TASKS"),
            translations = allLanguages("Client"),
        )

    private fun allLanguages(name: String): Set<Translation> =
        setOf(Translation(LanguageTag.of("pl"), name), Translation(LanguageTag.of("en"), name))

    private fun project(
        isPublic: Boolean = false,
        isActive: Boolean = true,
    ) = Project(
        name = "Apollo",
        ownerId = ownerId,
        isPublic = isPublic,
        isActive = isActive,
    )

    private fun membership(
        project: Project,
        role: ProjectRole,
    ) = ProjectMember(projectId = project.id, userId = memberId, roleId = role.id)

    @Nested
    inner class Owner {
        @Test
        fun `holds every permission on an active project`() {
            val project = project()
            ProjectPermission.entries.forEach { permission ->
                assertTrue(
                    ProjectAccessPolicy.permits(project, ownerId, null, null, permission).isPermitted,
                    "owner should hold $permission",
                )
            }
        }

        @Test
        fun `cannot mutate an archived project`() {
            val archived = project(isActive = false)
            val decision =
                ProjectAccessPolicy.permits(archived, ownerId, null, null, ProjectPermission.EDIT_PROJECT)

            assertIs<AccessDecision.Deny>(decision)
            assertEquals(ProjectError.PROJECT_ARCHIVED, decision.reason)
        }

        @Test
        fun `can still read an archived project`() {
            val archived = project(isActive = false)
            assertTrue(
                ProjectAccessPolicy
                    .permits(archived, ownerId, null, null, ProjectPermission.VIEW_PROJECT)
                    .isPermitted,
            )
        }
    }

    @Nested
    inner class Member {
        @Test
        fun `gets exactly what the role grants`() {
            val project = project()
            val member = membership(project, clientRole)

            ProjectPermission.entries.forEach { permission ->
                val expected = permission.name in clientRole.permissions
                assertEquals(
                    expected,
                    ProjectAccessPolicy.permits(project, memberId, member, clientRole, permission).isPermitted,
                    "client role and $permission",
                )
            }
        }

        @Test
        fun `is refused when the membership belongs to somebody else`() {
            val project = project()
            val someoneElsesMembership = membership(project, managerRole).copy(userId = strangerId)

            assertFalse(
                ProjectAccessPolicy
                    .permits(project, memberId, someoneElsesMembership, managerRole, ProjectPermission.EDIT_PROJECT)
                    .isPermitted,
            )
        }

        @Test
        fun `is refused when the loaded role is not the one the membership points at`() {
            val project = project()
            val member = membership(project, clientRole)

            assertFalse(
                ProjectAccessPolicy
                    .permits(project, memberId, member, managerRole, ProjectPermission.EDIT_PROJECT)
                    .isPermitted,
            )
        }

        @Test
        fun `loses write access once the role is deactivated`() {
            val project = project()
            val disabled = managerRole.copy(isActive = false)
            val member = membership(project, disabled)

            assertFalse(
                ProjectAccessPolicy
                    .permits(project, memberId, member, disabled, ProjectPermission.EDIT_PROJECT)
                    .isPermitted,
            )
        }
    }

    @Nested
    inner class Stranger {
        @Test
        fun `may view a public project but change nothing`() {
            val open = project(isPublic = true)

            assertTrue(
                ProjectAccessPolicy
                    .permits(open, strangerId, null, null, ProjectPermission.VIEW_PROJECT)
                    .isPermitted,
            )
            assertFalse(
                ProjectAccessPolicy
                    .permits(open, strangerId, null, null, ProjectPermission.EDIT_PROJECT)
                    .isPermitted,
            )
        }

        @Test
        fun `is refused everything on a private project`() {
            val private = project()
            ProjectPermission.entries.forEach { permission ->
                assertFalse(
                    ProjectAccessPolicy.permits(private, strangerId, null, null, permission).isPermitted,
                    "stranger should not hold $permission",
                )
            }
        }
    }

    @Nested
    inner class EffectivePermissions {
        @Test
        fun `match what permits reports, for every subject kind`() {
            val project = project(isPublic = true)
            val member = membership(project, clientRole)

            val cases =
                listOf(
                    Triple(ownerId, null, null),
                    Triple(memberId, member, clientRole),
                    Triple(strangerId, null, null),
                )

            cases.forEach { (subject, membership, role) ->
                val reported = ProjectAccessPolicy.permissionsOf(project, subject, membership, role)
                val evaluated =
                    ProjectPermission.entries
                        .filter { ProjectAccessPolicy.permits(project, subject, membership, role, it).isPermitted }
                        .mapTo(mutableSetOf()) { it.name } +
                        role
                            ?.permissions
                            .orEmpty()
                            .takeIf { role?.isActive == true && membership != null }
                            .orEmpty()

                assertEquals(evaluated, reported, "subject $subject")
            }
        }
    }
}
