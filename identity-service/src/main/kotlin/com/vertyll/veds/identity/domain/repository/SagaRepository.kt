package com.vertyll.veds.identity.domain.repository

import com.vertyll.veds.identity.domain.model.entity.Saga
import com.vertyll.veds.sharedinfrastructure.saga.repository.BaseSagaRepository
import org.springframework.stereotype.Repository

@Repository
interface SagaRepository : BaseSagaRepository<Saga>
