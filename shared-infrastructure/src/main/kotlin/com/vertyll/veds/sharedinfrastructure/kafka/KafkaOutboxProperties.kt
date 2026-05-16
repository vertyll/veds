package com.vertyll.veds.sharedinfrastructure.kafka

import com.vertyll.veds.sharedinfrastructure.kafka.contract.OutboxStatus.PROCESSING
import org.springframework.boot.context.properties.ConfigurationProperties
import java.time.Duration

/**
 * Externalized configuration for the Kafka transactional outbox.
 *
 * Bound from `veds.outbox.*` and consumed by [KafkaOutboxProcessor]. All
 * values have sane defaults so the outbox works out-of-the-box without any
 * configuration.
 *
 * Example:
 * ```yaml
 * veds:
 *   outbox:
 *     poll-interval: 5s
 *     batch-size: 100
 *     max-retries: 5
 *     retry-cooldown: 1m
 *     stuck-threshold: 5m
 * ```
 */
@ConfigurationProperties(prefix = "veds.outbox")
data class KafkaOutboxProperties(
    /** Scheduled poller interval (also used for the stuck-message reaper). */
    val pollInterval: Duration = Duration.ofSeconds(DEFAULT_POLL_INTERVAL_SECONDS),
    /** Max rows fetched (and locked) per poll cycle. */
    val batchSize: Int = DEFAULT_BATCH_SIZE,
    /** Maximum publishing attempts before a message is dead-lettered. */
    val maxRetries: Int = DEFAULT_MAX_RETRIES,
    /**
     * Minimum time between two attempts on the same message. Acts as an
     * exponential-backoff-less cooldown for the next dispatch.
     */
    val retryCooldown: Duration = Duration.ofMinutes(DEFAULT_RETRY_COOLDOWN_MINUTES),
    /**
     * A message stuck in [PROCESSING]
     * for longer than this is considered abandoned (publisher crashed) and
     * becomes eligible for dispatch again.
     */
    val stuckThreshold: Duration = Duration.ofMinutes(DEFAULT_STUCK_THRESHOLD_MINUTES),
) {
    companion object {
        const val DEFAULT_POLL_INTERVAL_SECONDS: Long = 5
        const val DEFAULT_BATCH_SIZE: Int = 100
        const val DEFAULT_MAX_RETRIES: Int = 5
        const val DEFAULT_RETRY_COOLDOWN_MINUTES: Long = 1
        const val DEFAULT_STUCK_THRESHOLD_MINUTES: Long = 5
    }
}
