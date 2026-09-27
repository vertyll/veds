package com.vertyll.veds.notification.application

import com.vertyll.veds.notification.domain.model.RecipientRef
import com.vertyll.veds.notification.domain.repository.RecipientDirectoryRepository
import java.util.UUID

internal class InMemoryRecipientDirectory : RecipientDirectoryRepository {
    val stored = linkedMapOf<UUID, RecipientRef>()

    fun given(vararg recipients: RecipientRef) = recipients.forEach { stored[it.userId] = it }

    override fun save(recipient: RecipientRef) = recipient.also { stored[it.userId] = it }

    override fun findById(userId: UUID) = stored[userId]

    override fun findByEmail(email: String) = stored.values.firstOrNull { it.email == email }
}
