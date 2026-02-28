package com.vertyll.veds.identity.infrastructure.service

import com.vertyll.veds.identity.domain.repository.UserRepository
import com.vertyll.veds.identity.infrastructure.exception.ApiException
import org.springframework.http.HttpStatus
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.stereotype.Service

@Service
class UserDetailsServiceImpl(
    private val userRepository: UserRepository,
) : UserDetailsService {
    private companion object {
        private const val USER_NOT_FOUND = "User not found"
    }

    override fun loadUserByUsername(username: String): UserDetails =
        userRepository
            .findByEmail(username)
            .orElseThrow {
                ApiException(
                    message = USER_NOT_FOUND,
                    status = HttpStatus.NOT_FOUND,
                )
            }
}
