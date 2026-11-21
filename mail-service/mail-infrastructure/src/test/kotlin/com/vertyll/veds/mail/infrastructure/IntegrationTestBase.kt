package com.vertyll.veds.mail.infrastructure

import org.junit.jupiter.api.Tag
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.Primary
import org.springframework.test.context.ActiveProfiles

@Tag("integration")
@SpringBootTest
@ActiveProfiles("test")
@Import(TestcontainersConfiguration::class, IntegrationTestBase.MailServer::class)
abstract class IntegrationTestBase protected constructor() {
    /** Stands in for the mail server, the one collaborator a test cannot run. */
    @TestConfiguration
    class MailServer {
        @Bean
        @Primary
        fun recordingMailSender() = RecordingMailSender()
    }
}
