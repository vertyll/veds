package com.vertyll.projecta.role.domain.service

import com.vertyll.projecta.role.domain.dto.RoleCreateDto
import com.vertyll.projecta.role.domain.dto.RoleResponseDto
import com.vertyll.projecta.role.domain.dto.RoleUpdateDto
import com.vertyll.projecta.role.domain.model.entity.Role
import com.vertyll.projecta.role.domain.model.entity.UserRole
import com.vertyll.projecta.role.domain.model.enums.SagaStepNames
import com.vertyll.projecta.role.domain.model.enums.SagaStepStatus
import com.vertyll.projecta.role.domain.model.enums.SagaTypes
import com.vertyll.projecta.role.domain.repository.RoleRepository
import com.vertyll.projecta.role.domain.repository.UserRoleRepository
import com.vertyll.projecta.role.infrastructure.exception.ApiException
import com.vertyll.projecta.role.infrastructure.kafka.RoleEventProducer
import com.vertyll.projecta.sharedinfrastructure.role.RoleType
import com.vertyll.projecta.sharedinfrastructure.util.OptimisticLockingValidator
import jakarta.annotation.PostConstruct
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class RoleService(
    private val roleRepository: RoleRepository,
    private val userRoleRepository: UserRoleRepository,
    private val roleEventProducer: RoleEventProducer,
    private val sagaManager: SagaManager,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    private companion object {
        private const val ERROR_UNKNOWN = "Unknown error"
        private const val ERROR_ROLE_NOT_FOUND = "Role not found"
        private const val DEFAULT_ROLE_FOR_ALL_USERS = "Default role for all users"
        private const val ADMIN_ROLE_WITH_ALL_PRIVILEGES = "Admin role with all privileges"
        private const val USER_ROLE_MAPPING_NOT_FOUND = "User-role mapping not found"
        private const val CANNOT_REMOVE_USER_ROLE = "Cannot remove USER role from user as it's their only role"
        private const val OPTIMISTIC_LOCKING_FAILURE = "Data has been modified by another transaction. Please refresh and try again."
    }

    /**
     * Initialize default roles on startup
     */
    @PostConstruct
    fun initializeDefaultRoles() {
        logger.info("Initializing default roles")
        try {
            if (!roleRepository.existsByName(RoleType.USER.value)) {
                val userRole =
                    Role.create(
                        name = RoleType.USER.value,
                        description = DEFAULT_ROLE_FOR_ALL_USERS,
                    )
                roleRepository.save(userRole)
                logger.info("Created default ${RoleType.USER.value} role")
            }

            if (!roleRepository.existsByName(RoleType.ADMIN.value)) {
                val adminRole =
                    Role.create(
                        name = RoleType.ADMIN.value,
                        description = ADMIN_ROLE_WITH_ALL_PRIVILEGES,
                    )
                roleRepository.save(adminRole)
                logger.info("Created default ${RoleType.ADMIN.value} role")
            }
        } catch (e: Exception) {
            logger.error("Error initializing default roles: ${e.message}", e)
        }
    }

    @Transactional
    fun createRole(dto: RoleCreateDto): RoleResponseDto {
        if (roleRepository.existsByName(dto.name)) {
            throw ApiException(
                message = "Role with name ${dto.name} already exists",
                status = HttpStatus.BAD_REQUEST,
            )
        }

        // Start a saga for role creation
        val saga =
            sagaManager.startSaga(
                sagaType = SagaTypes.ROLE_CREATION,
                payload = dto,
            )

        // Record the start of the role creation step
        sagaManager.recordSagaStep(
            sagaId = saga.id,
            stepName = SagaStepNames.CREATE_ROLE,
            status = SagaStepStatus.STARTED,
        )

        try {
            // Create the role
            val role =
                Role.create(
                    name = dto.name,
                    description = dto.description,
                )

            val savedRole = roleRepository.save(role)

            // Record successful completion of role creation step
            sagaManager.recordSagaStep(
                sagaId = saga.id,
                stepName = SagaStepNames.CREATE_ROLE,
                status = SagaStepStatus.COMPLETED,
                payload =
                    mapOf(
                        "roleId" to savedRole.id,
                        "name" to savedRole.name,
                    ),
            )

            // Send event
            try {
                roleEventProducer.sendRoleCreatedEvent(savedRole)
                sagaManager.completeSaga(saga.id)
            } catch (e: Exception) {
                logger.error("Failed to send role created event: ${e.message}", e)
                // We don't fail the saga here since the role was created successfully
                // Just log the event sending failure
            }

            return mapToDto(savedRole)
        } catch (e: Exception) {
            // Record failure
            sagaManager.recordSagaStep(
                sagaId = saga.id,
                stepName = SagaStepNames.CREATE_ROLE,
                status = SagaStepStatus.FAILED,
                payload = mapOf("error" to (e.message ?: ERROR_UNKNOWN)),
            )
            sagaManager.failSaga(saga.id, e.message ?: "Role creation failed")
            throw e
        }
    }

    @Transactional
    fun updateRole(
        id: Long,
        dto: RoleUpdateDto,
        headerVersion: Long? = null,
    ): RoleResponseDto {
        val role =
            roleRepository
                .findById(id)
                .orElseThrow {
                    ApiException(
                        message = ERROR_ROLE_NOT_FOUND,
                        status = HttpStatus.NOT_FOUND,
                    )
                }
        OptimisticLockingValidator.validate(role.version, headerVersion) {
            ApiException(
                message = OPTIMISTIC_LOCKING_FAILURE,
                status = HttpStatus.PRECONDITION_FAILED,
            )
        }

        if (dto.name != role.name && roleRepository.existsByName(dto.name)) {
            throw ApiException(
                message = "Role with name ${dto.name} already exists",
                status = HttpStatus.BAD_REQUEST,
            )
        }

        // Prevent updating of system roles
        val roleType = RoleType.fromString(role.name)
        if (roleType != null && role.name != dto.name) {
            throw ApiException(
                message = "Cannot change name of system role ${role.name}",
                status = HttpStatus.BAD_REQUEST,
            )
        }

        // Start saga for role update
        val saga =
            sagaManager.startSaga(
                sagaType = SagaTypes.ROLE_UPDATE,
                payload =
                    mapOf(
                        "roleId" to id,
                        "originalName" to role.name,
                        "originalDescription" to role.description,
                        "newName" to dto.name,
                        "newDescription" to dto.description,
                    ),
            )

        // Record start of update step
        sagaManager.recordSagaStep(
            sagaId = saga.id,
            stepName = SagaStepNames.UPDATE_ROLE,
            status = SagaStepStatus.STARTED,
        )

        try {
            val updatedRole =
                Role(
                    id = role.id,
                    name = dto.name,
                    description = dto.description,
                    version = headerVersion ?: role.version,
                )

            val savedRole = roleRepository.save(updatedRole)

            // Record successful completion of role update step
            sagaManager.recordSagaStep(
                sagaId = saga.id,
                stepName = SagaStepNames.UPDATE_ROLE,
                status = SagaStepStatus.COMPLETED,
                payload =
                    mapOf(
                        "roleId" to savedRole.id,
                        "name" to savedRole.name,
                        "originalData" to
                            mapOf(
                                "name" to role.name,
                                "description" to role.description,
                            ),
                    ),
            )

            // Send event
            try {
                roleEventProducer.sendRoleUpdatedEvent(savedRole)
                sagaManager.completeSaga(saga.id)
            } catch (e: Exception) {
                logger.error("Failed to send role updated event: ${e.message}", e)
                // We don't fail the saga here since the role was updated successfully
            }

            return mapToDto(savedRole)
        } catch (e: Exception) {
            // Record failure
            sagaManager.recordSagaStep(
                sagaId = saga.id,
                stepName = SagaStepNames.UPDATE_ROLE,
                status = SagaStepStatus.FAILED,
                payload = mapOf("error" to (e.message ?: ERROR_UNKNOWN)),
            )
            sagaManager.failSaga(saga.id, e.message ?: "Role update failed")
            throw e
        }
    }

    @Transactional(readOnly = true)
    fun getRoleById(id: Long): RoleResponseDto {
        val role =
            roleRepository
                .findById(id)
                .orElseThrow {
                    ApiException(
                        message = ERROR_ROLE_NOT_FOUND,
                        status = HttpStatus.NOT_FOUND,
                    )
                }
        return mapToDto(role)
    }

    @Transactional(readOnly = true)
    fun getRoleByName(name: String): RoleResponseDto {
        val role =
            roleRepository
                .findByName(name)
                .orElseThrow {
                    ApiException(
                        message = ERROR_ROLE_NOT_FOUND,
                        status = HttpStatus.NOT_FOUND,
                    )
                }
        return mapToDto(role)
    }

    @Transactional(readOnly = true)
    fun getAllRoles(): List<RoleResponseDto> = roleRepository.findAll().map { mapToDto(it) }

    @Transactional
    fun assignRoleToUser(
        userId: Long,
        roleName: String,
    ): UserRole {
        logger.info("Assigning role $roleName to user $userId")

        val role =
            roleRepository
                .findByName(roleName)
                .orElseThrow {
                    ApiException(
                        message = "Role $roleName not found",
                        status = HttpStatus.NOT_FOUND,
                    )
                }

        if (userRoleRepository.existsByUserIdAndRoleId(userId, role.id!!)) {
            logger.info("User $userId already has role $roleName")
            return userRoleRepository
                .findByUserIdAndRoleId(userId, role.id)
                .orElseThrow {
                    ApiException(
                        message = USER_ROLE_MAPPING_NOT_FOUND,
                        status = HttpStatus.INTERNAL_SERVER_ERROR,
                    )
                }
        }

        // Start saga for role assignment
        val saga =
            sagaManager.startSaga(
                sagaType = SagaTypes.ROLE_ASSIGNMENT,
                payload =
                    mapOf(
                        "userId" to userId,
                        "roleId" to role.id,
                        "roleName" to role.name,
                    ),
            )

        // Record start of assignment step
        sagaManager.recordSagaStep(
            sagaId = saga.id,
            stepName = SagaStepNames.ASSIGN_ROLE,
            status = SagaStepStatus.STARTED,
        )

        try {
            val userRole =
                UserRole(
                    userId = userId,
                    roleId = role.id,
                )

            val savedUserRole = userRoleRepository.save(userRole)

            // Record successful completion of role assignment step
            sagaManager.recordSagaStep(
                sagaId = saga.id,
                stepName = SagaStepNames.ASSIGN_ROLE,
                status = SagaStepStatus.COMPLETED,
                payload =
                    mapOf(
                        "userId" to userId,
                        "roleId" to role.id,
                        "roleName" to role.name,
                    ),
            )

            // Send role assigned event
            try {
                roleEventProducer.sendRoleAssignedEvent(savedUserRole, role.name)
                sagaManager.completeSaga(saga.id)
            } catch (e: Exception) {
                logger.error("Failed to send role assigned event: ${e.message}", e)
                // We don't fail the saga here since the role was assigned successfully
            }

            logger.info("Successfully assigned role $roleName to user $userId")
            return savedUserRole
        } catch (e: Exception) {
            // Record failure
            sagaManager.recordSagaStep(
                sagaId = saga.id,
                stepName = SagaStepNames.ASSIGN_ROLE,
                status = SagaStepStatus.FAILED,
                payload = mapOf("error" to (e.message ?: ERROR_UNKNOWN)),
            )
            sagaManager.failSaga(saga.id, e.message ?: "Role assignment failed")
            throw e
        }
    }

    @Transactional
    fun removeRoleFromUser(
        userId: Long,
        roleName: String,
    ) {
        logger.info("Removing role $roleName from user $userId")

        val role =
            roleRepository
                .findByName(roleName)
                .orElseThrow {
                    ApiException(
                        message = "Role $roleName not found",
                        status = HttpStatus.NOT_FOUND,
                    )
                }

        if (!userRoleRepository.existsByUserIdAndRoleId(userId, role.id!!)) {
            logger.info("User $userId doesn't have role $roleName")
            return
        }

        // If it's the USER role, check if it's the only role the user has
        if (roleName == RoleType.USER.value) {
            val userRoles = userRoleRepository.findByUserId(userId)
            if (userRoles.size == 1) {
                throw ApiException(
                    message = CANNOT_REMOVE_USER_ROLE,
                    status = HttpStatus.BAD_REQUEST,
                )
            }
        }

        // Start saga for role revocation
        val saga =
            sagaManager.startSaga(
                sagaType = SagaTypes.ROLE_REVOCATION,
                payload =
                    mapOf(
                        "userId" to userId,
                        "roleId" to role.id,
                        "roleName" to role.name,
                    ),
            )

        // Record start of revocation step
        sagaManager.recordSagaStep(
            sagaId = saga.id,
            stepName = SagaStepNames.REVOKE_ROLE,
            status = SagaStepStatus.STARTED,
        )

        try {
            userRoleRepository.deleteByUserIdAndRoleId(userId, role.id)

            // Record successful completion of role revocation step
            sagaManager.recordSagaStep(
                sagaId = saga.id,
                stepName = SagaStepNames.REVOKE_ROLE,
                status = SagaStepStatus.COMPLETED,
                payload =
                    mapOf(
                        "userId" to userId,
                        "roleId" to role.id,
                        "roleName" to role.name,
                    ),
            )

            // Send role revoked event
            try {
                roleEventProducer.sendRoleRevokedEvent(userId, role.id, role.name)
                sagaManager.completeSaga(saga.id)
            } catch (e: Exception) {
                logger.error("Failed to send role revoked event: ${e.message}", e)
                // We don't fail the saga here since the role was revoked successfully
            }

            logger.info("Successfully removed role $roleName from user $userId")
        } catch (e: Exception) {
            // Record failure
            sagaManager.recordSagaStep(
                sagaId = saga.id,
                stepName = SagaStepNames.REVOKE_ROLE,
                status = SagaStepStatus.FAILED,
                payload = mapOf("error" to (e.message ?: ERROR_UNKNOWN)),
            )
            sagaManager.failSaga(saga.id, e.message ?: "Role revocation failed")
            throw e
        }
    }

    @Transactional(readOnly = true)
    fun getRolesForUser(userId: Long): List<RoleResponseDto> {
        val userRoles = userRoleRepository.findByUserId(userId)
        val roleIds = userRoles.map { it.roleId }

        if (roleIds.isEmpty()) {
            return emptyList()
        }

        return roleRepository.findAllById(roleIds).map { mapToDto(it) }
    }

    @Transactional(readOnly = true)
    fun getUsersForRole(roleId: Long): List<Long> {
        val userRoles = userRoleRepository.findByRoleId(roleId)
        return userRoles.map { it.userId }
    }

    private fun mapToDto(role: Role): RoleResponseDto =
        RoleResponseDto(
            id = role.id!!,
            name = role.name,
            description = role.description,
            version = role.version,
        )
}
