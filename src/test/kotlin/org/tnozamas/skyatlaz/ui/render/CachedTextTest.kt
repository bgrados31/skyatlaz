package org.tnozamas.skyatlaz.ui.render

import net.minecraft.network.chat.Component
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test

/** The cache that keeps the render path free of allocations. */
class CachedTextTest {
    private fun counter(): Pair<CachedText<String>, () -> Int> {
        var builds = 0
        val text =
            CachedText<String> { value ->
                builds++
                Component.empty()
            }
        return text to { builds }
    }

    @Test
    fun `the same value returns the same component`() {
        val (text, builds) = counter()

        val first = text.of("a")
        val second = text.of("a")

        assertSame(first, second)
        assertEquals(1, builds())
    }

    @Test
    fun `a new value rebuilds the component`() {
        val (text, builds) = counter()

        text.of("a")
        text.of("b")

        assertEquals(2, builds())
    }

    @Test
    fun `invalidating forces a rebuild`() {
        val (text, builds) = counter()

        text.of("a")
        text.invalidate()
        text.of("a")

        assertEquals(2, builds())
    }
}
