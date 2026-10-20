package org.tnozamas.skyatlaz.feature.economy.heldprice

import org.tnozamas.skyatlaz.util.ItemPrices
import org.tnozamas.skyatlaz.util.PriceLine
import org.tnozamas.skyatlaz.util.PriceLines
import org.tnozamas.skyatlaz.util.PriceOptions

/**
 * What the held item card shows: the name of the item and the price lines under it.
 *
 * The title is plain text (the name the player sees on the item), the rows are translation keys with their values,
 * so the card can be built and tested without the game.
 */
data class HeldPriceContent(
    val title: String,
    val rows: List<PriceLine>,
) {
    companion object {
        val EMPTY = HeldPriceContent("", emptyList())
    }
}

/**
 * What the card of the item in the player's hand shows, and how. Pure: it turns the prices known for the item plus
 * the player's settings into the content of the card, so it can be unit tested without the game.
 */
object HeldPriceLogic {
    /**
     * The card for the item in hand. Empty when the hand is empty, when the item is not a SkyBlock item or when no
     * price is known for it: a card with a name and no price would only take space.
     */
    fun build(
        name: String?,
        id: String,
        prices: ItemPrices,
        options: PriceOptions,
    ): HeldPriceContent {
        val rows = PriceLines.build(prices, options)
        if (rows.isEmpty()) return HeldPriceContent.EMPTY
        val title = name?.takeIf { it.isNotBlank() } ?: id
        return HeldPriceContent(title, rows)
    }
}
