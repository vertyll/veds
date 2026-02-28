package com.vertyll.veds.mail

import com.vertyll.veds.sharedinfrastructure.config.SharedConfigAutoConfiguration
import com.vertyll.veds.sharedinfrastructure.kafka.KafkaConfigAutoConfiguration
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Import
import org.springframework.kafka.annotation.EnableKafka

@SpringBootApplication
@Import(
    SharedConfigAutoConfiguration::class,
    KafkaConfigAutoConfiguration::class,
)
@ComponentScan(
    "com.vertyll.veds.mail",
    "com.vertyll.veds.sharedinfrastructure",
)
@EnableKafka
class MailServiceApplication

fun main(args: Array<String>) {
    runApplication<MailServiceApplication>(*args)
}
