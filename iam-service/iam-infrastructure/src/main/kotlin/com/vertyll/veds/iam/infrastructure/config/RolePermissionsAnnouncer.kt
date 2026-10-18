package com.vertyll.veds.iam.infrastructure.config

import com.vertyll.veds.iam.application.port.outbound.RolePermissionsEventPublisherPort
import com.vertyll.veds.iam.domain.repository.RoleRepository
import org.slf4j.LoggerFactory
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
@Order(2)
internal class RolePermissionsAnnouncer(
    private val roleRepository: RoleRepository,
    private val eventPublisher: RolePermissionsEventPublisherPort,
) : ApplicationRunner {
    private val logger = LoggerFactory.getLogger(javaClass)

    @Transactional
    override fun run(args: ApplicationArguments) {
        val roles = roleRepository.findAll()
        roles.forEach(eventPublisher::publishChanged)
        logger.info("Announced what {} roles grant", roles.size)
    }
}
