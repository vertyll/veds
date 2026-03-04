package com.vertyll.veds.iam

import com.vertyll.veds.sharedinfrastructure.config.SharedConfigAutoConfiguration
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
)
@ComponentScan(
    "com.vertyll.veds.iam",
    "com.vertyll.veds.sharedinfrastructure",
)
@EnableJpaRepositories(
    "com.vertyll.veds.iam.domain.repository",
    "com.vertyll.veds.sharedinfrastructure.kafka",
)
@EntityScan(
    "com.vertyll.veds.iam.domain.model",
    "com.vertyll.veds.sharedinfrastructure.kafka",
)
@EnableKafka
class IAMServiceApplication

fun main(args: Array<String>) {
    runApplication<IAMServiceApplication>(*args)
}
