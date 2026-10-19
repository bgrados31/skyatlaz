package org.tnozamas.skyatlaz.core.debug

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SnapshotTest {
    private val hyperion =
        Snapshot.Item(
            id = "minecraft:iron_sword",
            count = 1,
            name = "§6Heroic Hyperion §d✪✪✪✪✪",
            lore = listOf("§7Damage: §c+260", "", "§6§lLEGENDARY DUNGEON SWORD"),
            customData = """{id:"HYPERION",uuid:"0f0e"}""",
        )

    private val snapshot =
        Snapshot(
            name = "hub",
            capturedAt = "2026-10-19T12:00:00Z",
            server = "mini12A",
            scoreboardTitle = "§e§lSKYBLOCK",
            scoreboard = listOf("§7 ⏣ §bVillage", "Purse: §61,234"),
            tabHeader = "§bYou are playing on §e§lMC.HYPIXEL.NET",
            tabFooter = null,
            tabList = listOf("§a§lPlayers §f(12)"),
            chat = listOf("§eWelcome to Hypixel SkyBlock!"),
            heldItem = hyperion,
            container = Snapshot.Container("Heart of the Mountain", 6, mapOf(4 to hyperion, 31 to hyperion.copy(count = 3))),
        )

    @Test
    fun `round trip keeps every field`() {
        assertEquals(snapshot, Snapshot.fromJson(snapshot.toJson()))
    }

    @Test
    fun `json is readable and keeps section signs unescaped`() {
        val json = snapshot.toJson()

        assertTrue("§6Heroic Hyperion" in json)
        assertTrue("\"format\": 1" in json)
    }

    @Test
    fun `older or partial dumps read with empty defaults`() {
        val partial = Snapshot.fromJson("""{"name":"old","scoreboard":["a"]}""")

        assertEquals(listOf("a"), partial.scoreboard)
        assertTrue(partial.tabList.isEmpty())
        assertNull(partial.heldItem)
        assertNull(partial.container)
    }
}
