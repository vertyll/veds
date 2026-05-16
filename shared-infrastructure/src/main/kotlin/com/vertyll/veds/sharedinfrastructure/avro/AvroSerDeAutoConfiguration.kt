package com.vertyll.veds.sharedinfrastructure.avro

import com.vertyll.veds.sharedinfrastructure.kafka.KafkaInfraProperties
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
@ConditionalOnProperty(
    name = ["spring.kafka.schema-registry-url"],
)
class AvroSerDeAutoConfiguration {
    @Bean
    fun avroPayloadSerializer(properties: KafkaInfraProperties) = AvroPayloadSerializer(properties)

    @Bean
    fun avroPayloadDeserializer(properties: KafkaInfraProperties) = AvroPayloadDeserializer(properties)
}
