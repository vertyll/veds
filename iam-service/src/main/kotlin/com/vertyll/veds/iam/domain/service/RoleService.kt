package com.vertyll.veds.iam.domain.service

import com.vertyll.veds.iam.domain.dto.RoleResponseDto
import com.vertyll.veds.iam.domain.model.entity.Role
import com.vertyll.veds.iam.domain.repository.RoleRepository
import com.vertyll.veds.iam.domain.repository.UserRepository
import com.vertyll.veds.iam.infrastructure.exception.ApiException
import com.vertyll.veds.sharedinfrastructure.util.OptimisticLockingValidator
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class RoleService(
    private val roleRepository: RoleRepository,
    private val userRepository: UserRepository,
) {
    private companion object {
        private const val ROLE_NOT_FOUND = "Role not found"
        private const val USER_NOT_FOUND = "User not found"
        private const val PRECONDITION_FAILED = "Precondition Failed: Version mismatch"
    }

    @Transactional(readOnly = true)
    fun getRoleById(id: Long): RoleResponseDto {
        val role =
            roleRepository
                .findById(id)
                .orElseThrow {
                    ApiException(
                        message = ROLE_NOT_FOUND,
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
                        message = ROLE_NOT_FOUND,
                        status = HttpStatus.NOT_FOUND,
                    )
                }
        return mapToDto(role)
    }

    @Transactional(readOnly = true)
    fun getAllRoles(): List<RoleResponseDto> = roleRepository.findAll().map { mapToDto(it) }

    @Transactional(readOnly = true)
    fun getRolesForUser(userId: Long): List<RoleResponseDto> {
        val user = getUser(userId)
        return user.roles.map { mapToDto(it) }
    }

    @Transactional
    fun assignRoleToUser(
        userId: Long,
        roleName: String,
        version: Long? = null,
    ) {
        val user = getUser(userId)

        OptimisticLockingValidator.validate(user.version, version) {
            ApiException(
                message = PRECONDITION_FAILED,
                status = HttpStatus.PRECONDITION_FAILED,
            )
        }

        val role =
            roleRepository
                .findByName(roleName)
                .orElseThrow {
                    ApiException(
                        message = ROLE_NOT_FOUND,
                        status = HttpStatus.NOT_FOUND,
                    )
                }
        user.addRole(role)
        userRepository.save(user)
    }

    @Transactional
    fun removeRoleFromUser(
        userId: Long,
        roleName: String,
        version: Long? = null,
    ) {
        val user =
            userRepository
                .findById(userId)
                .orElseThrow {
                    ApiException(
                        message = USER_NOT_FOUND,
                        status = HttpStatus.NOT_FOUND,
                    )
                }

        OptimisticLockingValidator.validate(user.version, version) {
            ApiException(
                message = PRECONDITION_FAILED,
                status = HttpStatus.PRECONDITION_FAILED,
            )
        }

        val role =
            roleRepository
                .findByName(roleName)
                .orElseThrow {
                    ApiException(
                        message = ROLE_NOT_FOUND,
                        status = HttpStatus.NOT_FOUND,
                    )
                }
        user.removeRole(role.id!!)
        userRepository.save(user)
    }

    private fun getUser(userId: Long) =
        userRepository
            .findById(userId)
            .orElseThrow {
                ApiException(
                    message = USER_NOT_FOUND,
                    status = HttpStatus.NOT_FOUND,
                )
            }

    private fun mapToDto(role: Role): RoleResponseDto =
        RoleResponseDto(
            id = role.id!!,
            name = role.name,
            description = role.description,
            version = role.version,
        )
}
