package com.vertyll.veds.identity.domain.repository

import com.vertyll.veds.identity.domain.model.entity.SagaStep
import com.vertyll.veds.sharedinfrastructure.saga.repository.BaseSagaStepRepository
import org.springframework.stereotype.Repository

@Repository
interface SagaStepRepository : BaseSagaStepRepository<SagaStep>
