package com.vertyll.veds.template.domain.repository

import com.vertyll.veds.sharedinfrastructure.saga.repository.BaseSagaRepository
import com.vertyll.veds.template.domain.model.entity.Saga
import org.springframework.stereotype.Repository

@Repository
interface SagaRepository : BaseSagaRepository<Saga>
