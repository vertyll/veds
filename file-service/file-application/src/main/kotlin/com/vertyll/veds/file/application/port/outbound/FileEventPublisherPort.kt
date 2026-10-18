package com.vertyll.veds.file.application.port.outbound

import com.vertyll.veds.file.domain.model.FileScope
import java.util.UUID

interface FileEventPublisherPort {
    fun publishFileConfirmed(
        fileId: UUID,
        scope: FileScope,
        scopeId: UUID?,
        ownerId: UUID,
    )

    fun publishFileDeleted(
        fileId: UUID,
        scope: FileScope,
        scopeId: UUID?,
    )
}
