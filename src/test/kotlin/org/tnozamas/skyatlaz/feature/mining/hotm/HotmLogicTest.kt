package org.tnozamas.skyatlaz.feature.mining.hotm

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.tnozamas.skyatlaz.api.profile.ProfileApi
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

    @Test
    fun `the card shows the perks of the tree with their level and their maximum`() {
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
    fun `the card shows the powder that is left`() {
        val card = card()

        assertTrue(card.lines.any { it.key == "mining.hotm.powder.mithril" && it.args == listOf("184.52k") })
    }

    @Test
    fun `the card can leave the powder out`() {
        val card = card(HotmOptions(showPowders = false, showAbility = false))

        assertTrue(card.lines.none { it.key.startsWith("mining.hotm.powder") })
        assertTrue(card.lines.none { it.key == "mining.hotm.ability" })
    }

    /** The card of the profile of the fixture, with the layout of the fixture. */
    private fun card(options: HotmOptions = HotmOptions()): HotmContent {
        val profile =
            ProfileApi.parseMining(
                requireNotNull(javaClass.getResource("/fixtures/profile/profile.json")).readText(),
                "069a79f4-0000-4000-8000-000000000000",
            )
        val layout =
            HotmLayout.parse(requireNotNull(javaClass.getResource("/fixtures/profile/hotmlayout.json")).readText())
        return HotmLogic.buildCard(profile, layout, options)
    }
}
