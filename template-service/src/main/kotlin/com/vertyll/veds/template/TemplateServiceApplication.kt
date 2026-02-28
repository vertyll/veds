package com.vertyll.veds.template

import com.vertyll.veds.sharedinfrastructure.config.SharedConfigAutoConfiguration
import com.vertyll.veds.sharedinfrastructure.kafka.KafkaConfigAutoConfiguration
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.persistence.autoconfigure.EntityScan
import org.springframework.boot.runApplication
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Import
import org.springframework.data.jpa.repository.config.EnableJpaRepositories
import org.springframework.kafka.annotation.EnableKafka

@SpringBootApplication
@Import(
    SharedConfigAutoConfiguration::class,
    KafkaConfigAutoConfiguration::class,
)
@ComponentScan(
    "com.vertyll.veds.template",
    "com.vertyll.veds.sharedinfrastructure",
)
@EnableJpaRepositories(
    "com.vertyll.veds.template.domain.repository",
    "com.vertyll.veds.sharedinfrastructure.kafka",
)
@EntityScan(
    "com.vertyll.veds.template.domain.model",
    "com.vertyll.veds.sharedinfrastructure.kafka",
)
@EnableKafka
class TemplateServiceApplication

fun main(args: Array<String>) {
    runApplication<TemplateServiceApplication>(*args)
}
