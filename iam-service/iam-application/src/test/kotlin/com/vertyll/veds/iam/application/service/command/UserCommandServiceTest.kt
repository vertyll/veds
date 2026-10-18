@file:OptIn(ExperimentalUuidApi::class)

package com.vertyll.veds.iam.application.service.command

import com.vertyll.veds.iam.application.InMemoryUserRepository
import com.vertyll.veds.iam.application.RecordingAuthEventPublisher
import com.vertyll.veds.iam.application.command.UpdateProfileCommand
import com.vertyll.veds.iam.application.user
import com.vertyll.veds.iam.domain.error.IamError
import com.vertyll.veds.sharederror.ApiException
import org.junit.jupiter.api.Test
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlin.uuid.toJavaUuid

internal class UserCommandServiceTest {
    private val users = InMemoryUserRepository()
    private val events = RecordingAuthEventPublisher()

    private val service = UserCommandService(users, events)

    private val existing = user().copy(version = 0L).also { users.given(it) }

    private fun profile(
        firstName: String = "Grace",
        avatarFileId: UUID? = null,
    ) = UpdateProfileCommand(
        firstName = firstName,
        lastName = "Hopper",
        avatarFileId = avatarFileId,
        phoneNumber = "+48 123 456 789",
        address = "Warsaw",
    )

    @Test
    fun `a profile update replaces every editable field`() {
        val avatar = Uuid.generateV7().toJavaUuid()

        service.updateProfile(existing.id!!, profile(avatarFileId = avatar), version = 0L)

        val saved = users.findById(existing.id!!)!!
        assertEquals("Grace", saved.firstName)
        assertEquals("Hopper", saved.lastName)
        assertEquals(avatar, saved.avatarFileId)
        assertEquals("Warsaw", saved.address)
    }

    @Test
    fun `the update is announced so other services can refresh their copy`() {
        service.updateProfile(existing.id!!, profile(), version = 0L)

        assertEquals(listOf("UserProfileUpdated(${existing.email})"), events.published)
    }

    @Test
    fun `an update against a stale version is refused`() {
        val error = assertFailsWith<ApiException> { service.updateProfile(existing.id!!, profile(), version = 9L) }

        assertEquals(IamError.USER_VERSION_MISMATCH, error.error)
        assertEquals("Ada", users.findById(existing.id!!)!!.firstName)
        assertTrue(events.published.isEmpty())
    }

    @Test
    fun `an unknown user cannot be updated`() {
        val error = assertFailsWith<ApiException> { service.updateProfile(404L, profile(), version = 0L) }

        assertEquals(IamError.USER_NOT_FOUND, error.error)
    }

    @Test
    fun `a user with no identity yet is saved but not announced`() {
        val local = user(id = 2L, email = "local@example.com", keycloakId = null).copy(version = 0L).also { users.given(it) }

        service.updateProfile(local.id!!, profile(), version = 0L)

        assertEquals("Grace", users.findById(local.id!!)!!.firstName)
        assertTrue(events.published.isEmpty())
    }
}
