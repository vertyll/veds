package com.vertyll.projecta.identity.domain.repository

import com.vertyll.projecta.identity.domain.model.entity.Saga
import com.vertyll.projecta.sharedinfrastructure.saga.repository.BaseSagaRepository
import org.springframework.stereotype.Repository

@Repository
interface SagaRepository : BaseSagaRepository<Saga>
