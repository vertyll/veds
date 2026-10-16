@file:OptIn(ExperimentalUuidApi::class)

package com.vertyll.veds.shared.messaging.event

import java.time.Instant
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * Tiny shared helpers used when assembling event payloads in outbound adapters.
 *
 * No marker interface for events is exported on purpose — see `README.md` for
 * the rationale (Avro-generated `SpecificRecord` classes already provide
 * everything we need, a custom `DomainEvent` interface would be dead code).
 */
object Events {
    /** A fresh time-ordered UUID (v7), as an `eventId`. */
    fun newId(): String = Uuid.generateV7().toString()

    /** Returns the current instant. Indirection kept for test stubbing convenience. */
    fun now(): Instant = Instant.now()
}
