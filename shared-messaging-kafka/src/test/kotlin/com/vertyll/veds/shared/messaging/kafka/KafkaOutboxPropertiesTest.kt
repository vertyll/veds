package com.vertyll.veds.shared.messaging.kafka

import org.junit.jupiter.api.Test
import java.time.Duration
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/** Settings the poller cannot run with are refused when the service starts, not discovered while it runs. */
internal class KafkaOutboxPropertiesTest {
    @Test
    fun `the defaults are accepted`() {
        val properties = KafkaOutboxProperties()

        assertEquals(Duration.ofSeconds(5), properties.pollInterval)
    }

    @Test
    fun `a poll interval of zero is refused`() {
        assertFailsWith<IllegalArgumentException> { KafkaOutboxProperties(pollInterval = Duration.ZERO) }
    }

    @Test
    fun `a negative retry cooldown is refused`() {
        assertFailsWith<IllegalArgumentException> { KafkaOutboxProperties(retryCooldown = Duration.ofSeconds(-1)) }
    }

    @Test
    fun `a send timeout of zero is refused`() {
        assertFailsWith<IllegalArgumentException> { KafkaOutboxProperties(sendTimeout = Duration.ZERO) }
    }

    /** Otherwise a message could be reclaimed and published again while its first send is still pending. */
    @Test
    fun `a send timeout not shorter than the stuck threshold is refused`() {
        assertFailsWith<IllegalArgumentException> {
            KafkaOutboxProperties(sendTimeout = Duration.ofMinutes(5), stuckThreshold = Duration.ofMinutes(5))
        }
    }
}
