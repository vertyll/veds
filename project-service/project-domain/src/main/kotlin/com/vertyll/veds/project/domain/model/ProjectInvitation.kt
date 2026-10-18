@file:OptIn(ExperimentalUuidApi::class)

package com.vertyll.veds.project.domain.model

import java.time.Duration
import java.time.Instant
import java.util.UUID
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlin.uuid.toJavaUuid

data class ProjectInvitation(
    val id: UUID = Uuid.generateV7().toJavaUuid(),
    val projectId: UUID,
    val inviteeEmail: String,
    val inviteeId: UUID? = null,
    val inviterId: UUID,
    val roleId: UUID,
    val status: InvitationStatus = InvitationStatus.PENDING,
    val expiresAt: Instant = Instant.now().plus(DEFAULT_VALIDITY),
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now(),
    val version: Long? = null,
) {
    init {
        require(inviteeEmail.isNotBlank()) { "invitee email must not be blank" }
    }

    val isPending: Boolean get() = status == InvitationStatus.PENDING

    fun hasExpiredAt(now: Instant): Boolean = isPending && now.isAfter(expiresAt)

    fun accept(acceptedBy: UUID): ProjectInvitation {
        check(isPending) { NOT_PENDING }
        return copy(
            status = InvitationStatus.ACCEPTED,
            inviteeId = acceptedBy,
            updatedAt = Instant.now(),
        )
    }

    fun reject(rejectedBy: UUID): ProjectInvitation {
        check(isPending) { NOT_PENDING }
        return copy(
            status = InvitationStatus.REJECTED,
            inviteeId = rejectedBy,
            updatedAt = Instant.now(),
        )
    }

    fun expire(): ProjectInvitation {
        check(isPending) { NOT_PENDING }
        return copy(status = InvitationStatus.EXPIRED, updatedAt = Instant.now())
    }

    companion object {
        private const val NOT_PENDING = "invitation is no longer pending"
        private val DEFAULT_VALIDITY: Duration = Duration.ofDays(7)

        fun create(
            projectId: UUID,
            inviteeEmail: String,
            inviterId: UUID,
            roleId: UUID,
        ): ProjectInvitation =
            ProjectInvitation(
                projectId = projectId,
                inviteeEmail = inviteeEmail,
                inviterId = inviterId,
                roleId = roleId,
            )
    }
}
