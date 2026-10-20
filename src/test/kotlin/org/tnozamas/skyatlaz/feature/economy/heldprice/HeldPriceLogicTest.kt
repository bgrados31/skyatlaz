package org.tnozamas.skyatlaz.feature.economy.heldprice

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.tnozamas.skyatlaz.util.ItemPrices
import org.tnozamas.skyatlaz.util.PriceLine
import org.tnozamas.skyatlaz.util.PriceOptions

/** The card of the item in hand, without the game. */
class HeldPriceLogicTest {
    private val prices = ItemPrices(bazaarSell = 12_300_000.0, lowestBin = 45_000_000.0)

    @Test
    fun `the card shows the name of the item and its price`() {
        val content = HeldPriceLogic.build("Enchanted Diamond", "ENCHANTED_DIAMOND", prices, PriceOptions())

        assertEquals("Enchanted Diamond", content.title)
        assertEquals(
            listOf(
                PriceLine("economy.price.bazaar", listOf("12.3M")),
                PriceLine("economy.price.lowest_bin", listOf("45M")),
            ),
            content.rows,
        )
    }

    @Test
    fun `the id is the name when the item has none`() {
        val content = HeldPriceLogic.build(null, "ENCHANTED_DIAMOND", prices, PriceOptions())

        assertEquals("ENCHANTED_DIAMOND", content.title)
    }

    @Test
    fun `an item without a known price shows nothing`() {
        val content = HeldPriceLogic.build("Enchanted Diamond", "ENCHANTED_DIAMOND", ItemPrices(), PriceOptions())

        assertEquals(HeldPriceContent.EMPTY, content)
    }

    @Test
    fun `a price that is turned off leaves the card empty`() {
        val content =
            HeldPriceLogic.build(
                "Enchanted Diamond",
                "ENCHANTED_DIAMOND",
                prices,
                PriceOptions(showBazaar = false, showAuction = false),
            )

        assertEquals(HeldPriceContent.EMPTY, content)
    }

    @Test
    fun `an empty name is not used as the title`() {
        val content = HeldPriceLogic.build("  ", "ENCHANTED_DIAMOND", prices, PriceOptions())

        assertEquals("ENCHANTED_DIAMOND", content.title)
        assertTrue(content.rows.isNotEmpty())
    }
}
