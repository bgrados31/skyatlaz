package org.tnozamas.skyatlaz.feature.mining.hotm

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.tnozamas.skyatlaz.core.debug.Snapshot
import org.tnozamas.skyatlaz.util.LegacyText

/** The perks of the Heart of the Mountain, read from a real capture of its menu. */
class HotmLogicTest {
    private val container: Snapshot.Container =
        requireNotNull(
            requireNotNull(javaClass.getResource("/fixtures/skyblock/hotm.json"))
                .readText()
                .let { Snapshot.fromJson(it).container },
        )

    private val perks: List<HotmPerk> = HotmLogic.parse(container.slots.values.toList())

    private fun perk(name: String): HotmPerk? = perks.firstOrNull { it.name == name }

    @Test
    fun `the tree of the owner is read`() {
        assertEquals("Heart of the Mountain", LegacyText.strip(container.title))

        assertTrue(perks.isNotEmpty())
    }

    @Test
    fun `a leveled perk knows its level and its maximum`() {
        val fortune = requireNotNull(perk("Mining Fortune"))

        assertEquals(47, fortune.level)
        assertEquals(50, fortune.maxLevel)
        assertEquals(HotmPerk.State.ACTIVE, fortune.state)
    }

    @Test
    fun `a perk at its maximum only says its level`() {
        val speed = requireNotNull(perk("Mining Speed"))

        assertEquals(50, speed.level)
        assertNull(speed.maxLevel)
    }

    @Test
    fun `the core of the mountain is a perk with a level`() {
        val core = requireNotNull(perk("Core of the Mountain"))

        assertEquals(3, core.level)
        assertEquals(10, core.maxLevel)
    }

    @Test
    fun `a pickaxe ability is told apart from the perks`() {
        val pickobulus = requireNotNull(perk("Pickobulus"))

        assertTrue(pickobulus.ability)
        assertNull(pickobulus.level)
        assertEquals(HotmPerk.State.SELECTED, pickobulus.state)
    }

    @Test
    fun `a locked perk is not active`() {
        val gemLover = requireNotNull(perk("Gem Lover"))

        assertEquals(HotmPerk.State.LOCKED, gemLover.state)
    }

    @Test
    fun `the tiers and the buttons are not perks`() {
        assertNull(perk("Tier 5"))
        assertNull(perk("Close"))
        assertNull(perk("Go Back"))
        assertNull(perk("Scroll Up"))
    }

    @Test
    fun `an item that is not a perk is ignored`() {
        val ignored = HotmLogic.parsePerk(Snapshot.Item("minecraft:paper", 1, "A crystal", listOf("A description"), null))

        assertNull(ignored)
    }
}
