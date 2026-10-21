package org.tnozamas.skyatlaz.api.repo

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

/** The layout of the mining tree, read from the community data. */
class HotmLayoutTest {
    private val layout: HotmLayout =
        requireNotNull(javaClass.getResource("/fixtures/skyblock/hotmlayout.json")).readText().let(HotmLayout::parse)

    @Test
    fun `the perks of the tree are read`() {
        val speed = requireNotNull(layout.perk("mining_speed"))

        assertEquals("Mining Speed", speed.name)
        assertEquals(50, speed.maxLevel)
    }

    @Test
    fun `the place of a perk in the tree is read`() {
        val speed = requireNotNull(layout.perk("mining_speed"))

        assertEquals(3, speed.x)
        assertEquals(9, speed.y)
    }

    @Test
    fun `a perk that the data does not know is not found`() {
        assertNull(layout.perk("not_a_perk"))
    }
}
