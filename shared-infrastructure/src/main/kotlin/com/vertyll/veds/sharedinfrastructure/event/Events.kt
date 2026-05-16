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
    fun newId(): String = UUID.randomUUID().toString()

    fun now(): Instant = Instant.now()
}
