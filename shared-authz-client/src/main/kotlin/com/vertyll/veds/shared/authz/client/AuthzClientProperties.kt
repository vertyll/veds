package com.vertyll.veds.shared.authz.client

import jakarta.validation.constraints.NotBlank
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.validation.annotation.Validated
import java.time.Duration

/**
 * @property baseUrl where `iam-service` lives. The call goes straight to the service
 *           inside the cluster: the gateway routes no `/internal` path, and registration
 *           is not a request any browser makes.
 * @property registrationRetryInterval how long to wait before trying again when
 *           iam-service is not answering yet. Registration is what fills this
 *           service's role projection, so giving up would leave every permission
 *           check failing closed until the next restart.
 *
 * Example:
 * ```yaml
 * application:
 *   authz:
 *     client:
 *       base-url: http://iam-service:8082
 *       registration-retry-interval: 15s
 * ```
 */
@ConfigurationProperties(prefix = "application.authz.client")
@Validated
data class AuthzClientProperties(
    @field:NotBlank
    val baseUrl: String,
    val registrationRetryInterval: Duration = DEFAULT_REGISTRATION_RETRY_INTERVAL,
) {
    init {
        require(!registrationRetryInterval.isNegative) { "application.authz.client.registration-retry-interval must not be negative" }
    }

    private companion object {
        private val DEFAULT_REGISTRATION_RETRY_INTERVAL: Duration = Duration.ofSeconds(15)
    }
}
