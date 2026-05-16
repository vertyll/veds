package com.vertyll.veds.iam.infrastructure.config

import com.vertyll.veds.iam.application.service.AuthCompensationService
import com.vertyll.veds.iam.infrastructure.persistence.entity.SagaJpaEntity
import com.vertyll.veds.iam.infrastructure.persistence.entity.SagaStepJpaEntity
import com.vertyll.veds.iam.infrastructure.persistence.repository.SagaJpaRepository
import com.vertyll.veds.iam.infrastructure.persistence.repository.SagaStepJpaRepository
import com.vertyll.veds.iam.infrastructure.saga.IamCompensationEventSerializer
import com.vertyll.veds.iam.infrastructure.saga.IamSagaCompensationHandler
import com.vertyll.veds.iam.infrastructure.saga.IamSagaCompensationStepFactory
import com.vertyll.veds.iam.infrastructure.saga.IamSagaCompensator
import com.vertyll.veds.iam.infrastructure.saga.IamSagaEntityFactory
import com.vertyll.veds.sharedinfrastructure.avro.AvroCompensationEventDeserializer
import com.vertyll.veds.sharedinfrastructure.avro.AvroPayloadDeserializer
import com.vertyll.veds.sharedinfrastructure.avro.AvroPayloadSerializer
import com.vertyll.veds.sharedinfrastructure.kafka.KafkaOutboxProcessor
import com.vertyll.veds.sharedinfrastructure.saga.service.CompensationEventDeserializer
import com.vertyll.veds.sharedinfrastructure.saga.service.CompensationEventSerializer
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
        compensationEventSerializer: CompensationEventSerializer,
        objectMapper: ObjectMapper,
    ): SagaEngine<SagaJpaEntity, SagaStepJpaEntity> =
        SagaEngine(
            sagaRepository = sagaRepository,
            sagaStepRepository = sagaStepRepository,
            kafkaOutboxProcessor = kafkaOutboxProcessor,
            objectMapper = objectMapper,
            entityFactory = IamSagaEntityFactory(),
            compensator = IamSagaCompensator(),
            compensationEventSerializer = compensationEventSerializer,
            compensationTopic = SAGA_COMPENSATION_TOPIC,
        )

    @Bean
    fun iamSagaCompensationEngine(
        sagaStepRepository: SagaStepJpaRepository,
        compensationEventDeserializer: CompensationEventDeserializer,
        authCompensationService: AuthCompensationService,
    ): SagaCompensationEngine<SagaStepJpaEntity> =
        SagaCompensationEngine(
            sagaStepRepository = sagaStepRepository,
            compensationEventDeserializer = compensationEventDeserializer,
            stepFactory = IamSagaCompensationStepFactory(),
            handler = IamSagaCompensationHandler(authCompensationService),
        )

    @Bean
    fun iamCompensationEventSerializer(avroPayloadSerializer: AvroPayloadSerializer): CompensationEventSerializer =
        IamCompensationEventSerializer(
            avroPayloadSerializer = avroPayloadSerializer,
            topic = SAGA_COMPENSATION_TOPIC,
        )

    @Bean
    fun iamCompensationEventDeserializer(avroPayloadDeserializer: AvroPayloadDeserializer): CompensationEventDeserializer =
        AvroCompensationEventDeserializer(
            avroPayloadDeserializer = avroPayloadDeserializer,
            topic = SAGA_COMPENSATION_TOPIC,
        )
}
