package com.vertyll.veds.sharedinfrastructure.kafka

import com.vertyll.veds.sharedinfrastructure.kafka.contract.OutboxMessageFactory
import com.vertyll.veds.sharedinfrastructure.kafka.contract.OutboxRepositoryPort
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * Registers [KafkaOutboxJpaAdapter] as the default implementation of the
 * outbox ports when JPA is on the classpath. Services backed by another
 * storage technology (MongoDB, Cassandra, …) provide their own
 * [OutboxRepositoryPort] / [OutboxMessageFactory] bean and this auto-config
 * steps aside via `@ConditionalOnMissingBean`.
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(name = ["jakarta.persistence.Entity"])
internal class JpaOutboxAdapterAutoConfiguration {
    @Bean
    @ConditionalOnMissingBean(value = [OutboxRepositoryPort::class, OutboxMessageFactory::class])
    fun kafkaOutboxJpaAdapter(repository: KafkaOutboxRepository): KafkaOutboxJpaAdapter = KafkaOutboxJpaAdapter(repository)
}
