package com.vertyll.projecta.sharedinfrastructure.saga.repository

import com.vertyll.projecta.sharedinfrastructure.saga.entity.BaseSaga
import com.vertyll.projecta.sharedinfrastructure.saga.enums.SagaStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.repository.NoRepositoryBean
import java.time.Instant
import java.util.Optional

@NoRepositoryBean
interface BaseSagaRepository<T : BaseSaga> : JpaRepository<T, String> {
    fun findByType(type: String): List<T>

    fun findByStatus(status: SagaStatus): List<T>

    fun findByTypeAndStatus(
        type: String,
        status: SagaStatus,
    ): List<T>

    fun findByIdAndType(
        id: String,
        type: String,
    ): Optional<T>

    fun findByStartedAtBefore(startedAt: Instant): List<T>

    fun findByStatusInAndStartedAtBefore(
        statuses: List<SagaStatus>,
        startedAt: Instant,
    ): List<T>
}
