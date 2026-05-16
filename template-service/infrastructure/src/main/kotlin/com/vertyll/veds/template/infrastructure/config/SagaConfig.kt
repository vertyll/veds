package com.vertyll.veds.template.infrastructure.config

import com.vertyll.veds.sharedinfrastructure.avro.AvroPayloadDeserializer
import com.vertyll.veds.sharedinfrastructure.avro.AvroPayloadSerializer
import com.vertyll.veds.sharedinfrastructure.kafka.KafkaOutboxProcessor
import com.vertyll.veds.sharedinfrastructure.saga.SagaCompensationTopic
import com.vertyll.veds.sharedinfrastructure.saga.SagaProperties
import com.vertyll.veds.sharedinfrastructure.saga.service.CompensationCommandDeserializer
import com.vertyll.veds.sharedinfrastructure.saga.service.CompensationCommandHandler
import com.vertyll.veds.sharedinfrastructure.saga.service.CompensationEventSerializer
import com.vertyll.veds.sharedinfrastructure.saga.service.DefaultSagaCompensationContext
import com.vertyll.veds.sharedinfrastructure.saga.service.SagaCompensationContext
import com.vertyll.veds.sharedinfrastructure.saga.service.SagaCompensationEngine
import com.vertyll.veds.sharedinfrastructure.saga.service.SagaCompensationRunner
import com.vertyll.veds.sharedinfrastructure.saga.service.SagaEngine
import com.vertyll.veds.sharedinfrastructure.saga.service.SagaWatchdog
import com.vertyll.veds.template.application.port.inbound.TemplateCompensationUseCase
import com.vertyll.veds.template.application.saga.model.TemplateCompensationCommand
import com.vertyll.veds.template.infrastructure.persistence.entity.SagaJpaEntity
import com.vertyll.veds.template.infrastructure.persistence.entity.SagaStepJpaEntity
import com.vertyll.veds.template.infrastructure.persistence.repository.SagaJpaRepository
import com.vertyll.veds.template.infrastructure.persistence.repository.SagaStepJpaRepository
import com.vertyll.veds.template.infrastructure.saga.AvroTemplateCompensationCommandTranslator
import com.vertyll.veds.template.infrastructure.saga.TemplateCompensationEventSerializer
import com.vertyll.veds.template.infrastructure.saga.TemplateSagaCompensationHandler
import com.vertyll.veds.template.infrastructure.saga.TemplateSagaCompensationStepFactory
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
        const val SAGA_COMPENSATION_TOPIC: String = SagaCompensationTopic.PREFIX + "template"
    }

    @Bean
    fun templateSagaCompensationContext(
        kafkaOutboxProcessor: KafkaOutboxProcessor,
        compensationEventSerializer: CompensationEventSerializer<TemplateCompensationCommand>,
        objectMapper: ObjectMapper,
    ): SagaCompensationContext<TemplateCompensationCommand> =
        DefaultSagaCompensationContext(
            kafkaOutboxProcessor = kafkaOutboxProcessor,
            compensationEventSerializer = compensationEventSerializer,
            compensationTopic = SAGA_COMPENSATION_TOPIC,
            objectMapper = objectMapper,
        )

    @Bean
    fun templateSagaCompensationRunner(
        sagaRepository: SagaJpaRepository,
        sagaStepRepository: SagaStepJpaRepository,
        compensationContext: SagaCompensationContext<TemplateCompensationCommand>,
    ): SagaCompensationRunner<SagaJpaEntity, SagaStepJpaEntity, TemplateCompensationCommand> =
        SagaCompensationRunner(
            sagaRepository = sagaRepository,
            sagaStepRepository = sagaStepRepository,
            compensator = TemplateSagaCompensator(),
            compensationContext = compensationContext,
        )

    @Bean
    fun templateSagaEngine(
        sagaRepository: SagaJpaRepository,
        sagaStepRepository: SagaStepJpaRepository,
        objectMapper: ObjectMapper,
        compensationRunner: SagaCompensationRunner<SagaJpaEntity, SagaStepJpaEntity, TemplateCompensationCommand>,
    ): SagaEngine<SagaJpaEntity, SagaStepJpaEntity> =
        SagaEngine(
            sagaRepository = sagaRepository,
            sagaStepRepository = sagaStepRepository,
            objectMapper = objectMapper,
            entityFactory = TemplateSagaEntityFactory(),
            compensationRunner = compensationRunner,
        )

    @Bean
    fun templateSagaCompensationEngine(
        sagaStepRepository: SagaStepJpaRepository,
        commandDeserializer: CompensationCommandDeserializer<TemplateCompensationCommand>,
        handler: CompensationCommandHandler<TemplateCompensationCommand>,
    ): SagaCompensationEngine<SagaStepJpaEntity, TemplateCompensationCommand> =
        SagaCompensationEngine(
            sagaStepRepository = sagaStepRepository,
            commandDeserializer = commandDeserializer,
            stepFactory = TemplateSagaCompensationStepFactory(),
            handler = handler,
        )

    @Bean
    fun templateCompensationCommandHandler(
        templateCompensationService: TemplateCompensationUseCase,
    ): CompensationCommandHandler<TemplateCompensationCommand> = TemplateSagaCompensationHandler(templateCompensationService)

    @Bean
    fun templateCompensationEventSerializer(
        avroPayloadSerializer: AvroPayloadSerializer,
    ): CompensationEventSerializer<TemplateCompensationCommand> =
        TemplateCompensationEventSerializer(
            avroPayloadSerializer = avroPayloadSerializer,
            topic = SAGA_COMPENSATION_TOPIC,
        )

    @Bean
    fun templateCompensationCommandDeserializer(
        avroPayloadDeserializer: AvroPayloadDeserializer,
    ): CompensationCommandDeserializer<TemplateCompensationCommand> =
        AvroTemplateCompensationCommandTranslator(
            avroPayloadDeserializer = avroPayloadDeserializer,
            topic = SAGA_COMPENSATION_TOPIC,
        )

    @Bean
    fun templateSagaWatchdog(
        sagaRepository: SagaJpaRepository,
        sagaEngine: SagaEngine<SagaJpaEntity, SagaStepJpaEntity>,
        sagaProperties: SagaProperties,
    ): SagaWatchdog<SagaJpaEntity, SagaStepJpaEntity> =
        SagaWatchdog(
            sagaRepository = sagaRepository,
            sagaEngine = sagaEngine,
            properties = sagaProperties,
        )
}
