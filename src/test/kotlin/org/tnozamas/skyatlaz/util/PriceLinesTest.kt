package org.tnozamas.skyatlaz.util

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** The price lines shared by the features that show prices, without the game. */
class PriceLinesTest {
    private val prices =
        ItemPrices(
            bazaarSell = 12_300_000.0,
            lowestBin = 45_000_000.0,
            npcSell = 1_200_000.0,
        )

    @Test
    fun `the bazaar and the auction are shown by default`() {
        assertEquals(
            listOf(
                PriceLine("economy.price.bazaar", listOf("12.3M")),
                PriceLine("economy.price.lowest_bin", listOf("45M")),
            ),
            PriceLines.build(prices, PriceOptions()),
        )
    }

    @Test
    fun `the npc price is only shown when it is asked for`() {
        val lines = PriceLines.build(prices, PriceOptions(showNpc = true))

        assertEquals(
            listOf(
                PriceLine("economy.price.bazaar", listOf("12.3M")),
                PriceLine("economy.price.lowest_bin", listOf("45M")),
                PriceLine("economy.price.npc", listOf("1.2M")),
            ),
            lines,
        )
    }

    @Test
    fun `a source that is turned off is not shown`() {
        val lines = PriceLines.build(prices, PriceOptions(showBazaar = false, showAuction = false))

        assertTrue(lines.isEmpty())
    }

    @Test
    fun `an unknown price is skipped`() {
        val lines = PriceLines.build(ItemPrices(bazaarSell = null), PriceOptions())

        assertTrue(lines.isEmpty())
    }

    @Test
    fun `a price of zero is skipped`() {
        val lines = PriceLines.build(ItemPrices(bazaarSell = 0.0), PriceOptions())

        assertTrue(lines.isEmpty())
    }

    @Test
    fun `grouped numbers are written the way hypixel writes them`() {
        val lines = PriceLines.build(prices, PriceOptions(numberStyle = NumberStyle.GROUPED))

        assertEquals(PriceLine("economy.price.bazaar", listOf("12,300,000")), lines.first())
    }
}
