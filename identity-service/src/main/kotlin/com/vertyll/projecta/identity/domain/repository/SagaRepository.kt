package com.vertyll.projecta.identity.domain.repository

import com.vertyll.projecta.identity.domain.model.entity.Saga
import com.vertyll.projecta.identity.domain.model.enums.SagaStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.time.Instant
import java.util.Optional

@Repository
interface SagaRepository : JpaRepository<Saga, String> {
    fun findByType(type: String): List<Saga>

    fun findByStatus(status: SagaStatus): List<Saga>

    fun findByTypeAndStatus(
        type: String,
        status: SagaStatus,
    ): List<Saga>

    fun findByIdAndType(
        id: String,
        type: String,
    ): Optional<Saga>

    fun findByStartedAtBefore(startedAt: Instant): List<Saga>

    fun findByStatusInAndStartedAtBefore(
        statuses: List<SagaStatus>,
        startedAt: Instant,
    ): List<Saga>
}
