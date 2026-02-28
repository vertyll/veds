package com.vertyll.veds.template.domain.repository

import com.vertyll.veds.sharedinfrastructure.saga.repository.BaseSagaStepRepository
import com.vertyll.veds.template.domain.model.entity.SagaStep
import org.springframework.stereotype.Repository

@Repository
interface SagaStepRepository : BaseSagaStepRepository<SagaStep>
