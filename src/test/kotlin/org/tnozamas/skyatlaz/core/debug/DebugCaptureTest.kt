package org.tnozamas.skyatlaz.core.debug

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/** The name of a captured dump, without the game. */
class DebugCaptureTest {
    @Test
    fun `a title becomes a file name`() {
        assertEquals("heart_of_the_mountain", DebugCapture.sanitize("Heart of the Mountain"))
    }

    @Test
    fun `the odd characters become underscores`() {
        assertEquals("dwarven_mines", DebugCapture.sanitize("Dwarven Mines!"))
        assertEquals("the_glacier", DebugCapture.sanitize("The Glacier"))
    }

    @Test
    fun `an empty title still has a name`() {
        assertEquals("capture", DebugCapture.sanitize(""))
        assertEquals("capture", DebugCapture.sanitize("   "))
    }

    @Test
    fun `the name is never longer than the store accepts`() {
        val name = DebugCapture.sanitize("a".repeat(80))

        assertEquals(SnapshotStore.NAME_PATTERN_MAX, name.length)
        assertEquals(true, SnapshotStore.NAME_PATTERN.matches(name))
    }
}
