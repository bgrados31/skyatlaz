package org.tnozamas.skyatlaz.feature.mining.hotm

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.tnozamas.skyatlaz.api.repo.HotmLayout
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

    private val layout: HotmLayout =
        requireNotNull(javaClass.getResource("/fixtures/skyblock/hotmlayout.json"))
            .readText()
            .let(HotmLayout::parse)

    @Test
    fun `the tree of the owner is read`() {
        assertEquals("Heart of the Mountain", LegacyText.strip(container.title))

        assertTrue(perks.isNotEmpty())
    }

    @Test
    fun `the profile of the service builds the tree`() {
        val fromProfile =
            HotmLogic.perksFromProfile(
                levels = mapOf("mining_speed" to 50, "mining_fortune" to 12),
                toggles = mapOf("mining_speed" to true),
                ability = "pickobulus",
                layout = layout,
            )

        val speed = fromProfile.first { it.name == "Mining Speed" }
        assertEquals(50, speed.level)
        assertEquals(50, speed.maxLevel)
        assertEquals(HotmPerk.State.ACTIVE, speed.state)

        val fortune = fromProfile.first { it.name == "Mining Fortune" }
        assertEquals(12, fortune.level)
        assertEquals(HotmPerk.State.LOCKED, fortune.state)

        val ability = fromProfile.first { it.state == HotmPerk.State.SELECTED }
        assertEquals("Pickobulus", ability.name)
    }

    @Test
    fun `without the layout of the tree the profile is not enough`() {
        val fromProfile =
            HotmLogic.perksFromProfile(
                levels = mapOf("mining_speed" to 50),
                toggles = emptyMap(),
                ability = null,
                layout = null,
            )

        assertTrue(fromProfile.isEmpty())
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

    @Test
    fun `the card shows the perks that matter with their level and their maximum`() {
        val card = card()

        val fortune = requireNotNull(card.lines.firstOrNull { it.args.contains("Mining Fortune") })

        assertEquals(listOf("Mining Fortune", "47", "50"), fortune.args)
    }

    @Test
    fun `the card shows the ability that is equipped`() {
        val card = card()

        assertTrue(card.lines.any { it.key == "mining.hotm.ability" && it.args == listOf("Pickobulus") })
    }

    @Test
    fun `the card can leave the ability out`() {
        val card = card(HotmOptions(showAbility = false))

        assertTrue(card.lines.none { it.key == "mining.hotm.ability" })
    }

    @Test
    fun `the maximum of a perk at its maximum comes from the layout`() {
        val card = card()

        val speed = requireNotNull(card.lines.firstOrNull { it.args.contains("Mining Speed") })

        assertEquals(listOf("Mining Speed", "50", "50"), speed.args)
    }

    /** The card of the tree of the fixture, with the layout of the fixture. */
    private fun card(options: HotmOptions = HotmOptions()): HotmContent {
        val layout =
            HotmLayout.parse(requireNotNull(javaClass.getResource("/fixtures/skyblock/hotmlayout.json")).readText())
        return HotmLogic.buildCard(perks, layout, options)
    }
}
