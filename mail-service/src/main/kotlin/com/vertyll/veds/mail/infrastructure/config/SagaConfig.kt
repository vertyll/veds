package com.vertyll.veds.mail.infrastructure.config

import com.vertyll.veds.mail.infrastructure.persistence.entity.SagaJpaEntity
import com.vertyll.veds.mail.infrastructure.persistence.entity.SagaStepJpaEntity
import com.vertyll.veds.mail.infrastructure.persistence.repository.SagaJpaRepository
import com.vertyll.veds.mail.infrastructure.persistence.repository.SagaStepJpaRepository
import com.vertyll.veds.mail.infrastructure.saga.MailCompensationEventSerializer
import com.vertyll.veds.mail.infrastructure.saga.MailSagaCompensator
import com.vertyll.veds.mail.infrastructure.saga.MailSagaEntityFactory
import com.vertyll.veds.sharedinfrastructure.avro.AvroPayloadSerializer
import com.vertyll.veds.sharedinfrastructure.kafka.KafkaOutboxProcessor
import com.vertyll.veds.sharedinfrastructure.saga.service.CompensationEventSerializer
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
        compensationEventSerializer: CompensationEventSerializer,
        objectMapper: ObjectMapper,
    ): SagaEngine<SagaJpaEntity, SagaStepJpaEntity> =
        SagaEngine(
            sagaRepository = sagaRepository,
            sagaStepRepository = sagaStepRepository,
            kafkaOutboxProcessor = kafkaOutboxProcessor,
            objectMapper = objectMapper,
            entityFactory = MailSagaEntityFactory(),
            compensator = MailSagaCompensator(),
            compensationEventSerializer = compensationEventSerializer,
            compensationTopic = SAGA_COMPENSATION_TOPIC,
        )

    @Bean
    fun mailCompensationEventSerializer(avroPayloadSerializer: AvroPayloadSerializer): CompensationEventSerializer =
        MailCompensationEventSerializer(
            avroPayloadSerializer = avroPayloadSerializer,
            topic = SAGA_COMPENSATION_TOPIC,
        )
}
