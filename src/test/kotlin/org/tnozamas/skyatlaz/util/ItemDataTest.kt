package org.tnozamas.skyatlaz.util

import net.minecraft.nbt.TagParser
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

class ItemDataTest {
    @ParameterizedTest
    @CsvSource(
        delimiter = '|',
        nullValues = ["-"],
        value = [
            "§6§lLEGENDARY DUNGEON SWORD|LEGENDARY|SWORD|true|false|false",
            "§d§l§ka§r §d§lMYTHIC ACCESSORY §d§l§ka|MYTHIC|ACCESSORY|false|false|true",
            "a SHINY MYTHIC DUNGEON CHESTPLATE a|MYTHIC|CHESTPLATE|true|true|true",
            "a VERY SPECIAL HATCESSORY a|VERY_SPECIAL|HATCESSORY|false|false|true",
            "SPECIAL|SPECIAL|-|false|false|false",
            "§9§lRARE|RARE|-|false|false|false",
            "EPIC WATER SHARD (ID E5)|EPIC|WATER SHARD|false|false|false",
            "Rarity: LEGENDARY|LEGENDARY|-|false|false|false",
            "LEGENDARY DUNGEON ITEM|LEGENDARY|ITEM|true|false|false",
        ],
    )
    fun `rarity lines`(
        line: String,
        rarity: Rarity,
        category: String?,
        dungeon: Boolean,
        shiny: Boolean,
        recombobulated: Boolean,
    ) {
        assertEquals(RarityLine(rarity, category, dungeon, shiny, recombobulated), RarityLine.parse(line))
    }

    @Test
    fun `non rarity lines are ignored`() {
        assertNull(RarityLine.parse("§7Damage: §c+260"))
        assertNull(RarityLine.parse(""))
        assertNull(RarityLine.parse("§eClick to upgrade!"))
        assertNull(RarityLine.parse("Commonly found in the hub"))
    }

    @Test
    fun `rarity from the bottom of the lore`() {
        val lore = listOf("§7Damage: §c+260", "", "§6§lLEGENDARY DUNGEON SWORD", "")
        assertEquals(Rarity.LEGENDARY, RarityLine.fromLore(lore)?.rarity)
    }

    @Test
    fun `rarities compare by tier`() {
        assertTrue(Rarity.MYTHIC > Rarity.LEGENDARY)
        assertTrue(Rarity.COMMON < Rarity.UNCOMMON)
    }

    @Test
    fun `custom data of a dungeon weapon`() {
        val tag =
            TagParser.parseCompoundFully(
                """{id:"HYPERION",uuid:"0f0e-11",modifier:"heroic",upgrade_level:5,rarity_upgrades:1,""" +
                    """hot_potato_count:15,enchantments:{ultimate_wise:5,sharpness:6},timestamp:1700000000000L}""",
            )

        val data = SkyBlockItemData.of(tag)

        assertEquals("HYPERION", data.id)
        assertEquals("heroic", data.reforge)
        assertEquals(5, data.stars)
        assertTrue(data.recombobulated)
        assertEquals(15, data.hotPotatoBooks)
        assertEquals(mapOf("ultimate_wise" to 5, "sharpness" to 6), data.enchantments)
        assertEquals(1_700_000_000_000L, data.timestamp)
    }

    @Test
    fun `custom data of a menu item is empty`() {
        val data = SkyBlockItemData.of(TagParser.parseCompoundFully("{}"))

        assertNull(data.id)
        assertEquals(0, data.stars)
        assertFalse(data.recombobulated)
        assertNull(data.timestamp)
    }

    @Test
    fun `older items store stars as dungeon_item_level`() {
        assertEquals(3, SkyBlockItemData.of(TagParser.parseCompoundFully("{id:\"BONZO_STAFF\",dungeon_item_level:3}")).stars)
    }
}
