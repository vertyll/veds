package com.vertyll.veds.shared.authz.client

import com.vertyll.veds.sharedauthz.PermissionCatalogue
import org.slf4j.LoggerFactory
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.stereotype.Component
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * Registers the module's declared permissions with `iam-service` at start-up.
 *
 * A service opts in by exposing a [PermissionCatalogue] bean.
 *
 * Registration happens off the start-up thread and keeps retrying, so a service
 * that boots before iam-service does not depend on the order the two came up in.
 * That matters beyond the panel's module list: registering is what makes iam
 * announce every role, which is how this service's projection is filled — a
 * service that never registered would fail closed on every permission it checks.
 *
 * Failure never stops the service. Refusing to boot would let a brief outage of
 * iam-service take down every other service with it.
 */
@Component
class PermissionCatalogueRegistrationRunner(
    private val catalogues: List<PermissionCatalogue>,
    private val registrar: PermissionCatalogueRegistrarAdapter,
    private val properties: AuthzClientProperties,
) : ApplicationRunner {
    private val logger = LoggerFactory.getLogger(javaClass)

    override fun run(args: ApplicationArguments) {
        if (catalogues.isEmpty()) {
            logger.debug("No permission catalogue declared, nothing to register")
            return
        }

        val worker = Executors.newSingleThreadScheduledExecutor { runnable -> Thread(runnable, "authz-registration") }
        worker.execute { registerUntilAccepted(worker) }
    }

    private fun registerUntilAccepted(worker: java.util.concurrent.ScheduledExecutorService) {
        val pending = catalogues.filterNot(::register)

        if (pending.isEmpty()) {
            worker.shutdown()
            return
        }

        logger.info(
            "{} catalogue(s) not registered yet; retrying in {}s",
            pending.size,
            properties.registrationRetryInterval.seconds,
        )
        worker.schedule(
            { registerUntilAccepted(worker) },
            properties.registrationRetryInterval.seconds,
            TimeUnit.SECONDS,
        )
    }

    private fun register(catalogue: PermissionCatalogue): Boolean =
        try {
            registrar.register(catalogue)
            logger.info("Registered {} permissions for module {}", catalogue.definitions.size, catalogue.module)
            true
        } catch (e: Exception) {
            logger.warn("Could not register the permission catalogue for {} yet: {}", catalogue.module, e.message)
            false
        }
}
