package com.vertyll.projecta.identity.domain.repository

import com.vertyll.projecta.identity.domain.model.entity.SagaStep
import com.vertyll.projecta.sharedinfrastructure.saga.repository.BaseSagaStepRepository
import org.springframework.stereotype.Repository

@Repository
interface SagaStepRepository : BaseSagaStepRepository<SagaStep>
