package com.vertyll.projecta.template.domain.repository

import com.vertyll.projecta.sharedinfrastructure.saga.repository.BaseSagaStepRepository
import com.vertyll.projecta.template.domain.model.entity.SagaStep
import org.springframework.stereotype.Repository

@Repository
interface SagaStepRepository : BaseSagaStepRepository<SagaStep>
