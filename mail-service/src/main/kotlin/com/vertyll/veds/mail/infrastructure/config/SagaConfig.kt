package com.vertyll.veds.mail.infrastructure.config

import com.vertyll.veds.mail.infrastructure.persistence.entity.SagaJpaEntity
import com.vertyll.veds.mail.infrastructure.persistence.entity.SagaStepJpaEntity
import com.vertyll.veds.mail.infrastructure.persistence.repository.SagaJpaRepository
import com.vertyll.veds.mail.infrastructure.persistence.repository.SagaStepJpaRepository
import com.vertyll.veds.mail.infrastructure.saga.MailSagaCompensator
import com.vertyll.veds.mail.infrastructure.saga.MailSagaEntityFactory
import com.vertyll.veds.sharedinfrastructure.kafka.KafkaOutboxProcessor
import com.vertyll.veds.sharedinfrastructure.saga.service.SagaEngine
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.EnableScheduling
import tools.jackson.databind.ObjectMapper

@Configuration
@EnableScheduling // Enable scheduling for outbox processing
internal class SagaConfig {
    companion object {
        const val SAGA_COMPENSATION_TOPIC = "saga-compensation-mail"
    }

    @Bean
    fun mailSagaEngine(
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
            entityFactory = MailSagaEntityFactory(),
            compensator = MailSagaCompensator(),
            compensationTopic = SAGA_COMPENSATION_TOPIC,
        )
}
