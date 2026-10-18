package com.vertyll.veds.project.infrastructure.persistence.query

import com.vertyll.veds.project.application.dto.ProjectCategoryResponse
import com.vertyll.veds.project.application.dto.ProjectListItemResponse
import com.vertyll.veds.project.application.dto.ProjectMemberResponse
import com.vertyll.veds.project.application.dto.ProjectResponse
import com.vertyll.veds.project.application.dto.ProjectStatusResponse
import com.vertyll.veds.project.application.port.outbound.ProjectQueryPort
import com.vertyll.veds.project.domain.model.LanguageTag
import com.vertyll.veds.project.domain.model.PageRequest
import com.vertyll.veds.project.domain.model.PageResult
import com.vertyll.veds.project.domain.model.ProjectSearchCriteria
import com.vertyll.veds.project.domain.model.ProjectSortField
import com.vertyll.veds.project.domain.model.Translation
import com.vertyll.veds.project.domain.model.resolveFor
import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import org.springframework.stereotype.Component
import java.time.Instant
import java.util.UUID

@Component
internal class ProjectQueryAdapter : ProjectQueryPort {
    private companion object {
        private const val MEMBER_ID = 0
        private const val MEMBER_PROJECT_ID = 1
        private const val MEMBER_USER_ID = 2
        private const val MEMBER_EMAIL = 3
        private const val MEMBER_FIRST_NAME = 4
        private const val MEMBER_LAST_NAME = 5
        private const val MEMBER_AVATAR_FILE_ID = 6
        private const val MEMBER_ROLE_ID = 7
        private const val MEMBER_ROLE_CODE = 8
        private const val MEMBER_ASSIGNED_AT = 9
        private const val MEMBER_VERSION = 10
    }

    @PersistenceContext
    private lateinit var entityManager: EntityManager

    override fun findProject(projectId: UUID): ProjectResponse? {
        val rows =
            entityManager
                .createQuery(
                    """
                    SELECT p.id, p.name, p.description, p.isPublic, p.isActive,
                           p.typeId, p.iconFileId, p.ownerId, p.createdAt, p.updatedAt, p.version,
                           p.hiddenWorkLogEnabled
                    FROM ProjectJpaEntity p
                    WHERE p.id = :projectId
                    """,
                    Array<Any>::class.java,
                ).setParameter("projectId", projectId)
                .resultList

        return rows.firstOrNull()?.let { r ->
            ProjectResponse(
                id = r[0] as UUID,
                name = r[1] as String,
                description = r[2] as String?,
                isPublic = r[3] as Boolean,
                isActive = r[4] as Boolean,
                typeId = r[5] as UUID?,
                iconFileId = r[6] as UUID?,
                ownerId = r[7] as UUID,
                createdAt = r[8] as Instant,
                updatedAt = r[9] as Instant,
                version = r[10] as Long?,
                hiddenWorkLogEnabled = r[11] as Boolean,
            )
        }
    }

    override fun searchProjects(
        criteria: ProjectSearchCriteria,
        pageRequest: PageRequest,
    ): PageResult<ProjectListItemResponse> {
        val where =
            """
            WHERE (
                p.ownerId = :requesterId
                OR EXISTS (
                    SELECT 1 FROM ProjectMemberJpaEntity m
                    WHERE m.projectId = p.id AND m.userId = :requesterId
                )
                OR (:includePublic = TRUE AND p.isPublic = TRUE)
            )
            AND (:onlyActive = FALSE OR p.isActive = TRUE)
            AND (:typeId IS NULL OR p.typeId = :typeId)
            AND (
                :searchTerm IS NULL
                OR LOWER(p.name) LIKE LOWER(CONCAT('%', CAST(:searchTerm AS string), '%'))
                OR LOWER(COALESCE(p.description, '')) LIKE LOWER(CONCAT('%', CAST(:searchTerm AS string), '%'))
            )
            """

        val order =
            when (criteria.sortBy) {
                ProjectSortField.NAME -> "p.name"
                ProjectSortField.CREATED_AT -> "p.createdAt"
                ProjectSortField.UPDATED_AT -> "p.updatedAt"
            } + if (criteria.sortDescending) " DESC" else " ASC"

        val rows =
            entityManager
                .createQuery(
                    """
                    SELECT p.id, p.name, p.description, p.isPublic, p.isActive,
                           p.iconFileId, p.typeId, p.createdAt, p.version,
                           (SELECT COUNT(m) FROM ProjectMemberJpaEntity m WHERE m.projectId = p.id)
                    FROM ProjectJpaEntity p
                    $where
                    ORDER BY $order
                    """,
                    Array<Any>::class.java,
                ).applyCriteria(criteria)
                .setFirstResult(pageRequest.offset.toInt())
                .setMaxResults(pageRequest.size)
                .resultList

        val total =
            entityManager
                .createQuery("SELECT COUNT(p) FROM ProjectJpaEntity p $where", Long::class.javaObjectType)
                .applyCriteria(criteria)
                .singleResult

        return PageResult(
            content =
                rows.map { r ->
                    ProjectListItemResponse(
                        id = r[0] as UUID,
                        name = r[1] as String,
                        description = r[2] as String?,
                        isPublic = r[3] as Boolean,
                        isActive = r[4] as Boolean,
                        iconFileId = r[5] as UUID?,
                        typeId = r[6] as UUID?,
                        memberCount = (r[9] as Long).toInt(),
                        createdAt = r[7] as Instant,
                        version = r[8] as Long?,
                    )
                },
            page = pageRequest.page,
            size = pageRequest.size,
            totalElements = total,
        )
    }

    override fun findMembers(
        projectId: UUID,
        language: LanguageTag,
    ): List<ProjectMemberResponse> {
        val rows =
            entityManager
                .createQuery(
                    """
                    SELECT m.id, m.projectId, m.userId, u.email, u.firstName, u.lastName, u.avatarFileId,
                           m.roleId, r.code, m.assignedAt, m.version
                    FROM ProjectMemberJpaEntity m
                    JOIN UserRefJpaEntity u ON u.userId = m.userId
                    JOIN ProjectRoleJpaEntity r ON r.id = m.roleId
                    WHERE m.projectId = :projectId
                    ORDER BY m.assignedAt
                    """,
                    Array<Any>::class.java,
                ).setParameter("projectId", projectId)
                .resultList

        val roleNames = translationsOf("project_role_translation", "project_role_id", language)
        val rolePermissions = permissionsByRole(rows.mapTo(mutableSetOf()) { it[MEMBER_ROLE_ID] as UUID })

        return rows.map { r ->
            val roleId = r[MEMBER_ROLE_ID] as UUID
            ProjectMemberResponse(
                id = r[MEMBER_ID] as UUID,
                projectId = r[MEMBER_PROJECT_ID] as UUID,
                userId = r[MEMBER_USER_ID] as UUID,
                email = r[MEMBER_EMAIL] as String,
                displayName =
                    listOfNotNull(r[MEMBER_FIRST_NAME] as String?, r[MEMBER_LAST_NAME] as String?)
                        .joinToString(" ")
                        .ifBlank { r[MEMBER_EMAIL] as String },
                avatarFileId = r[MEMBER_AVATAR_FILE_ID] as UUID?,
                roleId = roleId,
                roleCode = r[MEMBER_ROLE_CODE] as String,
                rolePermissions = rolePermissions[roleId].orEmpty(),
                roleName = roleNames[roleId]?.name ?: error("missing $language translation for role $roleId"),
                assignedAt = r[MEMBER_ASSIGNED_AT] as Instant,
                version = r[MEMBER_VERSION] as Long?,
            )
        }
    }

    private fun permissionsByRole(roleIds: Set<UUID>): Map<UUID, Set<String>> {
        if (roleIds.isEmpty()) return emptyMap()

        return entityManager
            .createNativeQuery(
                "SELECT project_role_id, permission FROM project_role_permission WHERE project_role_id IN (:roleIds)",
            ).setParameter("roleIds", roleIds)
            .resultList
            .map { it as Array<*> }
            .groupBy({ it[0] as UUID }, { it[1] as String })
            .mapValues { (_, permissions) -> permissions.toSet() }
    }

    override fun findCategories(
        projectId: UUID,
        language: LanguageTag,
    ): List<ProjectCategoryResponse> {
        val rows =
            entityManager
                .createQuery(
                    """
                    SELECT c.id, c.projectId, c.color, c.isActive, c.version
                    FROM ProjectCategoryJpaEntity c
                    WHERE c.projectId = :projectId
                    """,
                    Array<Any>::class.java,
                ).setParameter("projectId", projectId)
                .resultList

        val all = allTranslationsOf("project_category_translation", "project_category_id")

        return rows.map { r ->
            val id = r[0] as UUID
            val translations = all[id].orEmpty()
            val resolved = translations.resolveFor(language)
            ProjectCategoryResponse(
                id = id,
                projectId = r[1] as UUID,
                name = resolved.name,
                nameLanguage = resolved.language.value,
                color = r[2] as String,
                isActive = r[3] as Boolean,
                translations = translations,
                version = r[4] as Long?,
            )
        }
    }

    override fun findStatuses(
        projectId: UUID,
        language: LanguageTag,
    ): List<ProjectStatusResponse> {
        val rows =
            entityManager
                .createQuery(
                    """
                    SELECT s.id, s.projectId, s.color, s.isActive, s.version
                    FROM ProjectStatusJpaEntity s
                    WHERE s.projectId = :projectId
                    """,
                    Array<Any>::class.java,
                ).setParameter("projectId", projectId)
                .resultList

        val all = allTranslationsOf("project_status_translation", "project_status_id")

        return rows.map { r ->
            val id = r[0] as UUID
            val translations = all[id].orEmpty()
            val resolved = translations.resolveFor(language)
            ProjectStatusResponse(
                id = id,
                projectId = r[1] as UUID,
                name = resolved.name,
                nameLanguage = resolved.language.value,
                color = r[2] as String,
                isActive = r[3] as Boolean,
                translations = translations,
                version = r[4] as Long?,
            )
        }
    }

    private fun allTranslationsOf(
        table: String,
        ownerColumn: String,
    ): Map<UUID, Set<Translation>> {
        @Suppress("UNCHECKED_CAST")
        val rows =
            entityManager
                .createNativeQuery("SELECT $ownerColumn, language, name, description FROM $table")
                .resultList as List<Array<Any?>>

        return rows
            .groupBy { it[0] as UUID }
            .mapValues { (_, group) ->
                group
                    .map {
                        Translation(
                            language = LanguageTag.of(it[1] as String),
                            name = it[2] as String,
                            description = it[3] as String?,
                        )
                    }.toSet()
            }
    }

    private fun translationsOf(
        table: String,
        ownerColumn: String,
        language: LanguageTag,
    ): Map<UUID, Translation> =
        allTranslationsOf(table, ownerColumn)
            .mapNotNull { (id, translations) -> translations.firstOrNull { it.language == language }?.let { id to it } }
            .toMap()

    private fun <T> jakarta.persistence.TypedQuery<T>.applyCriteria(criteria: ProjectSearchCriteria) =
        setParameter("requesterId", criteria.requesterId)
            .setParameter("includePublic", criteria.includePublic)
            .setParameter("onlyActive", criteria.onlyActive)
            .setParameter("typeId", criteria.typeId)
            .setParameter("searchTerm", criteria.searchTerm)
}
