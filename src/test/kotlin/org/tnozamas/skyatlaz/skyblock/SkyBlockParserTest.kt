package org.tnozamas.skyatlaz.skyblock

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.tnozamas.skyatlaz.core.debug.Snapshot

/** Real scoreboard/tab dumps from Hypixel (`/skyatlaz debug dump`), captured 2026-10-19. */
class SkyBlockParserTest {
    private fun parse(name: String): SkyBlockState {
        val json = requireNotNull(javaClass.getResource("/fixtures/skyblock/$name.json")) { "missing fixture $name" }.readText()
        return SkyBlockParser.parse(Snapshot.fromJson(json).toSources())
    }

    @Test
    fun `hub dump reads area, profile, purse, bits, bank and level`() {
        val state = parse("hub")

        assertTrue(state.onSkyBlock)
        assertEquals("Hub", state.area)
        assertEquals("Village", state.subArea)
        assertEquals("mega24B", state.serverId)
        assertEquals("Mango", state.profile)
        assertEquals(85, state.level)
        assertEquals(0.4, state.levelProgress!!, 1e-9)
        assertEquals(638_769_221L, state.purse)
        assertEquals(32_222L, state.bits)
        assertEquals(1_100_000L, state.bank)
    }

    @Test
    fun `private island dump reads its own area and server`() {
        val state = parse("island")

        assertEquals("Private Island", state.area)
        assertEquals("Your Island", state.subArea)
        assertEquals("mini12AC", state.serverId)
        assertEquals("Mango", state.profile)
    }

    @Test
    fun `dwarven mines dump reads sub area and server`() {
        val state = parse("mines")

        assertEquals("Dwarven Mines", state.area)
        assertEquals("The Forge", state.subArea)
        assertEquals("mini71CU", state.serverId)
    }

    @Test
    fun `deep caverns dump reads its sub area and server`() {
        val state = parse("deep_caverns")

        assertEquals("Deep Caverns", state.area)
        assertEquals("Deep Caverns", state.subArea)
        assertEquals("mini81BD", state.serverId)
        assertEquals("Mango", state.profile)
    }

    @Test
    fun `garden dump reads its area and sub area`() {
        val state = parse("garden")

        assertEquals("Garden", state.area)
        // The sidebar also carries the plot number ("The Garden x8"), so only the name is checked.
        assertTrue(state.subArea!!.startsWith("The Garden"))
        assertEquals("mini221J", state.serverId)
    }

    @Test
    fun `crystal hollows dump reads the area and the jungle sub area`() {
        val state = parse("crystal_hollows")

        assertEquals("Crystal Hollows", state.area)
        assertEquals("Jungle", state.subArea)
        assertEquals("mini183CC", state.serverId)
    }

    @Test
    fun `catacombs dump reads the dungeon name and the difficulty`() {
        val state = parse("catacombs")

        assertEquals("The Catacombs (E)", state.subArea)
        assertEquals("Mango", state.profile)
    }

    @Test
    fun `empty sources are not on skyblock`() {
        val state = SkyBlockParser.parse(SkyBlockSources(null, emptyList(), null, null, emptyList()))

        assertEquals(SkyBlockState.EMPTY, state)
        assertFalse(state.onSkyBlock)
        assertNull(state.area)
    }

    @Test
    fun `tab area alone still counts as skyblock`() {
        val state = SkyBlockParser.parse(SkyBlockSources(null, emptyList(), null, null, listOf("Area: §7Hub")))

        assertTrue(state.onSkyBlock)
        assertEquals("Hub", state.area)
    }

    private fun Snapshot.toSources() = SkyBlockSources(scoreboardTitle, scoreboard, tabHeader, tabFooter, tabList)
}
