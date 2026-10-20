package org.tnozamas.skyatlaz.ui.hud

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class HudPlacementTest {
    @Test
    fun `position is a fraction of the free space`() {
        val placement = HudPlacement("test", defaultX = 0.0, defaultY = 0.0, defaultScale = 1.0)

        placement.moveTo(0.0, 0.0)
        assertEquals(0, placement.resolveX(1920, 100))
        assertEquals(0, placement.resolveY(1080, 40))

        placement.moveTo(1.0, 1.0)
        assertEquals(1820, placement.resolveX(1920, 100))
        assertEquals(1040, placement.resolveY(1080, 40))

        placement.moveTo(0.5, 0.5)
        assertEquals(910, placement.resolveX(1920, 100))
        assertEquals(520, placement.resolveY(1080, 40))
    }

    @Test
    fun `scale changes the free space`() {
        val placement = HudPlacement("test", defaultX = 1.0, defaultY = 1.0, defaultScale = 2.0)

        assertEquals(100, placement.scaledWidth(50))
        assertEquals(1820, placement.resolveX(1920, 50))
    }

    @Test
    fun `values are clamped and invalid input is sanitized`() {
        val placement = HudPlacement("test", defaultX = 0.5, defaultY = 0.5, defaultScale = 1.0)

        placement.moveTo(-3.0, 4.0)
        assertEquals(0.0, placement.x)
        assertEquals(1.0, placement.y)

        placement.setScale(99.0)
        assertEquals(HudPlacement.MAX_SCALE, placement.scale)
        placement.setScale(0.0)
        assertEquals(HudPlacement.MIN_SCALE, placement.scale)

        val invalid = HudPlacement("test", 0.0, 0.0, 1.0, x = Double.NaN, y = Double.NaN, scale = Double.NaN)
        assertEquals(0.0, invalid.x)
        assertEquals(0.0, invalid.y)
        assertEquals(1.0, invalid.scale)
    }

    @Test
    fun `elements larger than the screen stay at the origin`() {
        val placement = HudPlacement("test", 0.5, 0.5, 1.0)

        assertEquals(0, placement.resolveX(100, 200))
        assertEquals(0, placement.resolveY(100, 200))
    }

    @Test
    fun `reset restores the declared layout and notifies once`() {
        var changes = 0
        val placement = HudPlacement("test", defaultX = 0.1, defaultY = 0.2, defaultScale = 1.25, onChange = { changes++ })

        placement.moveTo(0.9, 0.8)
        placement.setScale(2.0)
        assertEquals(2, changes)

        placement.reset()
        assertEquals(0.1, placement.x)
        assertEquals(0.2, placement.y)
        assertEquals(1.25, placement.scale)
        assertEquals(3, changes)
    }

    @Test
    fun `setting the same values does not notify`() {
        var changes = 0
        val placement = HudPlacement("test", 0.0, 0.0, 1.0, onChange = { changes++ })

        placement.moveTo(0.0, 0.0)
        placement.setScale(1.0)
        placement.reset()
        assertEquals(0, changes)
    }
}
