package com.vertyll.veds.mail.domain.repository

import com.vertyll.veds.mail.domain.model.entity.Saga
import com.vertyll.veds.sharedinfrastructure.saga.repository.BaseSagaRepository
import org.springframework.stereotype.Repository

@Repository
interface SagaRepository : BaseSagaRepository<Saga>
