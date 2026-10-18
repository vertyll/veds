package com.vertyll.veds.mail.infrastructure.config

import com.vertyll.veds.mail.application.saga.model.MailCompensationCommand
import com.vertyll.veds.mail.infrastructure.persistence.entity.SagaJpaEntity
import com.vertyll.veds.mail.infrastructure.persistence.entity.SagaStepJpaEntity
import com.vertyll.veds.mail.infrastructure.persistence.repository.SagaJpaRepository
import com.vertyll.veds.mail.infrastructure.persistence.repository.SagaStepJpaRepository
import com.vertyll.veds.mail.infrastructure.saga.MailCompensationEventSerializer
import com.vertyll.veds.mail.infrastructure.saga.MailSagaCompensator
import com.vertyll.veds.mail.infrastructure.saga.MailSagaEntityFactory
import com.vertyll.veds.shared.messaging.avro.AvroPayloadSerializer
import com.vertyll.veds.shared.messaging.kafka.KafkaOutboxProcessor
import com.vertyll.veds.shared.saga.SagaProcessPort
import com.vertyll.veds.shared.saga.engine.CompensationEventSerializer
import com.vertyll.veds.shared.saga.engine.DefaultSagaCompensationContext
import com.vertyll.veds.shared.saga.engine.SagaCompensationContext
import com.vertyll.veds.shared.saga.engine.SagaCompensationRunner
import com.vertyll.veds.shared.saga.engine.SagaCompensationTopic
import com.vertyll.veds.shared.saga.engine.SagaEngine
import com.vertyll.veds.shared.saga.engine.SagaProcessAdapter
import com.vertyll.veds.shared.saga.engine.SagaProperties
import com.vertyll.veds.shared.saga.engine.SagaWatchdog
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.EnableScheduling
import tools.jackson.databind.ObjectMapper

@Configuration
@EnableScheduling
internal class SagaConfig {
    companion object {
        const val SAGA_COMPENSATION_TOPIC: String = SagaCompensationTopic.PREFIX + "mail"
    }

    @Bean
    fun mailSagaCompensationContext(
        kafkaOutboxProcessor: KafkaOutboxProcessor,
        compensationEventSerializer: CompensationEventSerializer<MailCompensationCommand>,
        objectMapper: ObjectMapper,
    ): SagaCompensationContext<MailCompensationCommand> =
        DefaultSagaCompensationContext(
            kafkaOutboxProcessor = kafkaOutboxProcessor,
            compensationEventSerializer = compensationEventSerializer,
            compensationTopic = SAGA_COMPENSATION_TOPIC,
            objectMapper = objectMapper,
        )

    @Bean
    fun mailSagaCompensationRunner(
        sagaRepository: SagaJpaRepository,
        sagaStepRepository: SagaStepJpaRepository,
        compensationContext: SagaCompensationContext<MailCompensationCommand>,
    ): SagaCompensationRunner<SagaJpaEntity, SagaStepJpaEntity, MailCompensationCommand> =
        SagaCompensationRunner(
            sagaRepository = sagaRepository,
            sagaStepRepository = sagaStepRepository,
            compensator = MailSagaCompensator(),
            compensationContext = compensationContext,
        )

    @Bean
    fun mailSagaEngine(
        sagaRepository: SagaJpaRepository,
        sagaStepRepository: SagaStepJpaRepository,
        objectMapper: ObjectMapper,
        compensationRunner: SagaCompensationRunner<SagaJpaEntity, SagaStepJpaEntity, MailCompensationCommand>,
    ): SagaEngine<SagaJpaEntity, SagaStepJpaEntity> =
        SagaEngine(
            sagaRepository = sagaRepository,
            sagaStepRepository = sagaStepRepository,
            objectMapper = objectMapper,
            entityFactory = MailSagaEntityFactory(),
            compensationRunner = compensationRunner,
        )

    @Bean
    fun mailSagaProcessPort(engine: SagaEngine<SagaJpaEntity, SagaStepJpaEntity>): SagaProcessPort = SagaProcessAdapter(engine)

    @Bean
    fun mailCompensationEventSerializer(
        avroPayloadSerializer: AvroPayloadSerializer,
    ): CompensationEventSerializer<MailCompensationCommand> =
        MailCompensationEventSerializer(
            avroPayloadSerializer = avroPayloadSerializer,
            topic = SAGA_COMPENSATION_TOPIC,
        )

    @Bean
    fun mailSagaWatchdog(
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
