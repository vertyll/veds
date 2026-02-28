package com.vertyll.projecta.template.domain.repository

import com.vertyll.projecta.sharedinfrastructure.saga.repository.BaseSagaRepository
import com.vertyll.projecta.template.domain.model.entity.Saga
import org.springframework.stereotype.Repository

@Repository
interface SagaRepository : BaseSagaRepository<Saga>
