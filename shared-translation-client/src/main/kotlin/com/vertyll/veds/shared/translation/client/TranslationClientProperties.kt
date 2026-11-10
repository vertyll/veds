package com.vertyll.veds.shared.translation.client

import jakarta.validation.constraints.NotBlank
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.validation.annotation.Validated
import java.time.Duration

/**
 * @property baseUrl where `translation-service` lives. This call goes straight to the
 *           service inside the cluster, not through the gateway: the gateway routes no
 *           `/internal` path, and registration is not a request any browser makes.
 * @property registrationRetryInterval how long to wait before trying again when
 *           translation-service is not answering yet. A key that never registered
 *           renders as the key itself to every reader, so giving up after one
 *           attempt would leave the interface in that state until the next restart.
 *
 * Example:
 * ```yaml
 * application:
 *   translation:
 *     client:
 *       base-url: http://translation-service:8087
 *       registration-retry-interval: 15s
 * ```
 */
@ConfigurationProperties(prefix = "application.translation.client")
@Validated
data class TranslationClientProperties(
    @field:NotBlank
    val baseUrl: String,
    val registrationRetryInterval: Duration = DEFAULT_REGISTRATION_RETRY_INTERVAL,
) {
    init {
        require(!registrationRetryInterval.isNegative) { "application.translation.client.registration-retry-interval must not be negative" }
    }

    private companion object {
        private val DEFAULT_REGISTRATION_RETRY_INTERVAL: Duration = Duration.ofSeconds(15)
    }
}
