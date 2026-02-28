package com.vertyll.projecta.identity.infrastructure.config

import com.vertyll.projecta.identity.domain.model.entity.Saga
import com.vertyll.projecta.identity.domain.model.entity.SagaStep
import com.vertyll.projecta.sharedinfrastructure.kafka.KafkaOutbox
import org.springframework.boot.persistence.autoconfigure.EntityScan
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.EnableScheduling

@Configuration
@EntityScan(basePackageClasses = [Saga::class, SagaStep::class, KafkaOutbox::class])
@EnableScheduling // Enable scheduling for outbox processing
class SagaConfig
