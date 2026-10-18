package com.vertyll.veds.shared.saga.engine.persistence

import com.vertyll.veds.shared.saga.SagaStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.repository.NoRepositoryBean
import java.time.Instant

/**
 * JPA flavor of [SagaRepositoryPort]. Concrete service repositories extend
 * this interface, and Spring Data JPA generates implementations that
 * simultaneously satisfy the persistence-agnostic port.
 *
 * Adding a new persistence flavor (e.g. MongoDB) only requires implementing
 * [SagaRepositoryPort] directly with the target technology — the saga engine
 * is unchanged.
 */
@NoRepositoryBean
interface BaseSagaRepository<T : BaseSaga<T>> :
    JpaRepository<T, String>,
    SagaRepositoryPort<T> {
    override fun findByType(type: String): List<T>

    override fun findByStatus(status: SagaStatus): List<T>

    override fun findByTypeAndStatus(
        type: String,
        status: SagaStatus,
    ): List<T>

    override fun findByStartedAtBefore(startedAt: Instant): List<T>

    override fun findByStatusInAndStartedAtBefore(
        statuses: List<SagaStatus>,
        startedAt: Instant,
    ): List<T>

    override fun findByStatusInAndUpdatedAtBefore(
        statuses: List<SagaStatus>,
        updatedAt: Instant,
    ): List<T>

    override fun findOneById(id: String): T? = findById(id).orElse(null)
}
