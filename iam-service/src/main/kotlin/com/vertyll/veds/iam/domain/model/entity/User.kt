package com.vertyll.veds.iam.domain.model.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.JoinTable
import jakarta.persistence.ManyToMany
import jakarta.persistence.Table
import jakarta.persistence.Version
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.UserDetails
import java.time.Instant

@Entity
@Table(
    name = "\"user\"",
    indexes = [
        jakarta.persistence.Index(name = "idx_user_email", columnList = "email"),
    ],
)
class User(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
    @Column(nullable = false, unique = true)
    private var email: String,
    @Column(nullable = false)
    private var password: String,
    @Column(nullable = false)
    var firstName: String,
    @Column(nullable = false)
    var lastName: String,
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "user_role_mapping",
        joinColumns = [JoinColumn(name = "user_id")],
        inverseJoinColumns = [JoinColumn(name = "role_id")],
    )
    var roles: MutableSet<Role> = mutableSetOf(),
    @Column(nullable = false)
    var enabled: Boolean = false,
    @Column(nullable = true)
    var profilePicture: String? = null,
    @Column(nullable = true)
    var phoneNumber: String? = null,
    @Column(nullable = true)
    var address: String? = null,
    @Column(nullable = false)
    val createdAt: Instant = Instant.now(),
    @Column(nullable = false)
    var updatedAt: Instant = Instant.now(),
    @Version
    val version: Long? = null,
) : UserDetails {
    constructor() : this(
        id = null,
        email = "",
        password = "",
        firstName = "",
        lastName = "",
        roles = mutableSetOf(),
        enabled = false,
        version = null,
    )

    override fun getAuthorities(): Collection<GrantedAuthority> = roles.map { SimpleGrantedAuthority("ROLE_${it.name}") }

    override fun getUsername(): String = email

    override fun getPassword(): String = password

    fun setPassword(newPassword: String) {
        this.password = newPassword
        this.updatedAt = Instant.now()
    }

    fun getEmail(): String = email

    fun setEmail(newEmail: String) {
        this.email = newEmail
        this.updatedAt = Instant.now()
    }

    fun addRole(role: Role) {
        if (role.id != null && roles.none { it.id == role.id }) {
            roles.add(role)
            updatedAt = Instant.now()
        }
    }

    fun removeRole(roleId: Long) {
        val roleToRemove = roles.find { it.id == roleId }
        if (roleToRemove != null) {
            roles.remove(roleToRemove)
            updatedAt = Instant.now()
        }
    }

    override fun isAccountNonExpired(): Boolean = true

    override fun isAccountNonLocked(): Boolean = true

    override fun isCredentialsNonExpired(): Boolean = true

    override fun isEnabled(): Boolean = enabled

    @Suppress("kotlin:S107")
    companion object {
        fun create(
            email: String,
            password: String,
            firstName: String,
            lastName: String,
            enabled: Boolean = false,
            profilePicture: String? = null,
            phoneNumber: String? = null,
            address: String? = null,
        ): User =
            User(
                email = email,
                password = password,
                firstName = firstName,
                lastName = lastName,
                enabled = enabled,
                profilePicture = profilePicture,
                phoneNumber = phoneNumber,
                address = address,
            )
    }
}
