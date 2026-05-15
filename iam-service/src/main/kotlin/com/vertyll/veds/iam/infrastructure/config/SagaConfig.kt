package com.vertyll.veds.iam.infrastructure.config

import com.vertyll.veds.iam.application.service.AuthCompensationService
import com.vertyll.veds.iam.infrastructure.persistence.entity.SagaJpaEntity
import com.vertyll.veds.iam.infrastructure.persistence.entity.SagaStepJpaEntity
import com.vertyll.veds.iam.infrastructure.persistence.repository.SagaJpaRepository
import com.vertyll.veds.iam.infrastructure.persistence.repository.SagaStepJpaRepository
import com.vertyll.veds.iam.infrastructure.saga.IamSagaCompensationHandler
import com.vertyll.veds.iam.infrastructure.saga.IamSagaCompensationStepFactory
import com.vertyll.veds.iam.infrastructure.saga.IamSagaCompensator
import com.vertyll.veds.iam.infrastructure.saga.IamSagaEntityFactory
import com.vertyll.veds.sharedinfrastructure.kafka.KafkaOutboxProcessor
import com.vertyll.veds.sharedinfrastructure.saga.service.SagaCompensationEngine
import com.vertyll.veds.sharedinfrastructure.saga.service.SagaEngine
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.EnableScheduling
import tools.jackson.databind.ObjectMapper

@Configuration
@EnableScheduling
internal class SagaConfig {
    companion object {
        const val SAGA_COMPENSATION_TOPIC = "saga-compensation-iam"
    }

    @Bean
    fun iamSagaEngine(
        sagaRepository: SagaJpaRepository,
        sagaStepRepository: SagaStepJpaRepository,
        kafkaOutboxProcessor: KafkaOutboxProcessor,
        objectMapper: ObjectMapper,
    ): SagaEngine<SagaJpaEntity, SagaStepJpaEntity> =
        SagaEngine(
            sagaRepository = sagaRepository,
            sagaStepRepository = sagaStepRepository,
            kafkaOutboxProcessor = kafkaOutboxProcessor,
            objectMapper = objectMapper,
            entityFactory = IamSagaEntityFactory(),
            compensator = IamSagaCompensator(),
            compensationTopic = SAGA_COMPENSATION_TOPIC,
        )

    @Bean
    fun iamSagaCompensationEngine(
        sagaStepRepository: SagaStepJpaRepository,
        objectMapper: ObjectMapper,
        authCompensationService: AuthCompensationService,
    ): SagaCompensationEngine<SagaStepJpaEntity> =
        SagaCompensationEngine(
            sagaStepRepository = sagaStepRepository,
            objectMapper = objectMapper,
            stepFactory = IamSagaCompensationStepFactory(),
            handler = IamSagaCompensationHandler(authCompensationService),
        )
}
