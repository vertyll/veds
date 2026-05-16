package com.vertyll.veds.sharedinfrastructure.saga

import com.vertyll.veds.sharedinfrastructure.saga.enums.SagaStatus.AWAITING_RESPONSE
import com.vertyll.veds.sharedinfrastructure.saga.enums.SagaStatus.COMPENSATING
import com.vertyll.veds.sharedinfrastructure.saga.enums.SagaStatus.COMPENSATION_FAILED
import org.springframework.boot.context.properties.ConfigurationProperties
import java.time.Duration

/**
 * Externalized configuration for the saga engine and its watchdog.
 *
 * Bound from `veds.saga.*`.
 *
 * Example:
 * ```yaml
 * veds:
 *   saga:
 *     await-response-timeout: 30m
 *     watchdog-interval: 1m
 *     compensation-retry-cooldown: 5m
 * ```
 */
@ConfigurationProperties(prefix = "veds.saga")
data class SagaProperties(
    /**
     * After a saga has been in
     * [AWAITING_RESPONSE]
     * for longer than this, the watchdog automatically fails it with reason
     * `"timeout"`, triggering compensation.
     */
    val awaitResponseTimeout: Duration = Duration.ofMinutes(DEFAULT_AWAIT_RESPONSE_TIMEOUT_MINUTES),
    /** Scheduled interval at which the saga watchdog runs. */
    val watchdogInterval: Duration = Duration.ofMinutes(DEFAULT_WATCHDOG_INTERVAL_MINUTES),
    /**
     * Minimum time between two compensation attempts on the same saga.
     * Sagas stuck in
     * [COMPENSATING]
     * or that ended in
     * [COMPENSATION_FAILED]
     * are eligible for another compensation attempt once this cooldown
     * elapses (based on `updatedAt`).
     */
    val compensationRetryCooldown: Duration = Duration.ofMinutes(DEFAULT_COMPENSATION_RETRY_COOLDOWN_MINUTES),
) {
    companion object {
        const val DEFAULT_AWAIT_RESPONSE_TIMEOUT_MINUTES: Long = 30
        const val DEFAULT_WATCHDOG_INTERVAL_MINUTES: Long = 1
        const val DEFAULT_COMPENSATION_RETRY_COOLDOWN_MINUTES: Long = 5
    }
}
