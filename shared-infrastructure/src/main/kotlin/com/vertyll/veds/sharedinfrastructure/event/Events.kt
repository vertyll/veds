package com.vertyll.veds.sharedinfrastructure.event

import java.time.Instant
import java.util.UUID

/**
 * Tiny shared helpers used when assembling event payloads in outbound adapters.
 *
 * No marker interface for events is exported on purpose — see `README.md` for
 * the rationale (Avro-generated `SpecificRecord` classes already provide
 * everything we need, a custom `DomainEvent` interface would be dead code).
 */
object Events {
    /** Returns a fresh UUID v4 string suitable for use as an `eventId`. */
    fun newId(): String = UUID.randomUUID().toString()

    /** Returns the current instant. Indirection kept for test stubbing convenience. */
    fun now(): Instant = Instant.now()
}
