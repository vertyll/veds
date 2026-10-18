package com.vertyll.veds.notification.domain.repository

import com.vertyll.veds.notification.domain.model.RecipientRef
import java.util.UUID

interface RecipientDirectoryRepository {
    fun save(recipient: RecipientRef): RecipientRef

    fun findById(userId: UUID): RecipientRef?

    fun findByEmail(email: String): RecipientRef?
}
