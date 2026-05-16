package com.vertyll.veds.template.infrastructure.config

import com.vertyll.veds.sharedinfrastructure.avro.AvroPayloadSerializer
import com.vertyll.veds.sharedinfrastructure.kafka.KafkaOutboxProcessor
import com.vertyll.veds.sharedinfrastructure.saga.service.CompensationEventSerializer
import com.vertyll.veds.sharedinfrastructure.saga.service.SagaEngine
import com.vertyll.veds.template.infrastructure.persistence.entity.SagaJpaEntity
import com.vertyll.veds.template.infrastructure.persistence.entity.SagaStepJpaEntity
import com.vertyll.veds.template.infrastructure.persistence.repository.SagaJpaRepository
import com.vertyll.veds.template.infrastructure.persistence.repository.SagaStepJpaRepository
import com.vertyll.veds.template.infrastructure.saga.TemplateCompensationEventSerializer
import com.vertyll.veds.template.infrastructure.saga.TemplateSagaCompensator
import com.vertyll.veds.template.infrastructure.saga.TemplateSagaEntityFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.EnableScheduling
import tools.jackson.databind.ObjectMapper

@Configuration
@EnableScheduling // Enable scheduling for outbox processing
internal class SagaConfig {
    companion object {
        const val SAGA_COMPENSATION_TOPIC = "saga-compensation-template"
    }

    @Bean
    fun templateSagaEngine(
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
            entityFactory = TemplateSagaEntityFactory(),
            compensator = TemplateSagaCompensator(),
            compensationEventSerializer = compensationEventSerializer,
            compensationTopic = SAGA_COMPENSATION_TOPIC,
        )

    @Bean
    fun templateCompensationEventSerializer(avroPayloadSerializer: AvroPayloadSerializer): CompensationEventSerializer =
        TemplateCompensationEventSerializer(
            avroPayloadSerializer = avroPayloadSerializer,
            topic = SAGA_COMPENSATION_TOPIC,
        )
}
