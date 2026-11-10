package com.vertyll.veds.shared.messaging.kafka

import com.vertyll.veds.shared.messaging.kafka.contract.OutboxStatus.PROCESSING
import jakarta.validation.constraints.Positive
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.validation.annotation.Validated
import java.time.Duration

/**
 * Externalized configuration for the Kafka transactional outbox.
 *
 * Bound from `application.outbox.*` and consumed by [KafkaOutboxProcessor]. All
 * values have sane defaults so the outbox works out-of-the-box without any
 * configuration.
 *
 * Example:
 * ```yaml
 * application:
 *   outbox:
 *     poll-interval: 5s
 *     batch-size: 100
 *     max-retries: 5
 *     retry-cooldown: 1m
 *     stuck-threshold: 5m
 *     send-timeout: 45s
 * ```
 */
@ConfigurationProperties(prefix = "application.outbox")
@Validated
data class KafkaOutboxProperties(
    /** Delay between the end of one poll and the start of the next. */
    val pollInterval: Duration = Duration.ofSeconds(DEFAULT_POLL_INTERVAL_SECONDS),
    /** Max rows fetched (and locked) per poll cycle. */
    @field:Positive
    val batchSize: Int = DEFAULT_BATCH_SIZE,
    /** Maximum publishing attempts before a message is dead-lettered. */
    @field:Positive
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
    /**
     * How long the poller waits for the broker to acknowledge one message before it counts
     * the attempt as failed. Longer than the producer's own `max.block.ms` plus
     * `delivery.timeout.ms`, so the producer reports its error first, and well below
     * [stuckThreshold], so a message is never reclaimed while its send is still pending.
     */
    val sendTimeout: Duration = Duration.ofSeconds(DEFAULT_SEND_TIMEOUT_SECONDS),
) {
    init {
        require(pollInterval > Duration.ZERO) { "application.outbox.poll-interval must be positive" }
        require(!retryCooldown.isNegative) { "application.outbox.retry-cooldown must not be negative" }
        require(sendTimeout > Duration.ZERO) { "application.outbox.send-timeout must be positive" }
        require(sendTimeout < stuckThreshold) { "application.outbox.send-timeout must be shorter than stuck-threshold" }
    }

    private companion object {
        private const val DEFAULT_POLL_INTERVAL_SECONDS: Long = 5
        private const val DEFAULT_BATCH_SIZE: Int = 100
        private const val DEFAULT_MAX_RETRIES: Int = 5
        private const val DEFAULT_RETRY_COOLDOWN_MINUTES: Long = 1
        private const val DEFAULT_STUCK_THRESHOLD_MINUTES: Long = 5
        private const val DEFAULT_SEND_TIMEOUT_SECONDS: Long = 45
    }
}
