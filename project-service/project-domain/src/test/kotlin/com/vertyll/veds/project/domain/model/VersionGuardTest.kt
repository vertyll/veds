package com.vertyll.veds.project.domain.model

import org.junit.jupiter.api.Test
import kotlin.test.assertFailsWith

class VersionGuardTest {
    private class Mismatch : RuntimeException()

    @Test
    fun `passes when the versions agree`() {
        VersionGuard.requireMatch(currentVersion = 3, expectedVersion = 3) { Mismatch() }
    }

    @Test
    fun `throws the supplied error when they differ`() {
        assertFailsWith<Mismatch> {
            VersionGuard.requireMatch(currentVersion = 4, expectedVersion = 3) { Mismatch() }
        }
    }

    @Test
    fun `skips the check when no expectation was supplied`() {
        VersionGuard.requireMatch(currentVersion = 7, expectedVersion = null) { Mismatch() }
    }

    @Test
    fun `treats a null current version as a mismatch when one was expected`() {
        assertFailsWith<Mismatch> {
            VersionGuard.requireMatch(currentVersion = null, expectedVersion = 1) { Mismatch() }
        }
    }
}
