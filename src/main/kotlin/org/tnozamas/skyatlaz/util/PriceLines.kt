package org.tnozamas.skyatlaz.util

/** The prices known for one item. A null price means "not known for this item". */
data class ItemPrices(
    val bazaarSell: Double? = null,
    val lowestBin: Double? = null,
    val npcSell: Double? = null,
)

/** Which prices the player wants to see and how they are written. */
data class PriceOptions(
    val showBazaar: Boolean = true,
    val showAuction: Boolean = true,
    val showNpc: Boolean = false,
    val numberStyle: NumberStyle = NumberStyle.COMPACT,
)

/** One price line: the key to translate and the values to fill it with. */
data class PriceLine(
    val key: String,
    val args: List<String>,
)

/**
 * The price lines of an item, shared by every feature that shows prices (the item tooltip and the held item card).
 * Two features must not import each other, so the logic that turns prices into lines lives here.
 *
 * Pure: it turns the prices known for an item plus the player's settings into the lines to show, so it can be unit
 * tested without the game. A line is a translation key and its arguments, never a finished sentence: the text belongs
 * to the language file and the numbers to the price data.
 */
object PriceLines {
    /**
     * The lines that show the price of an item, in reading order. Empty when nothing is known or nothing was asked
     * for.
     */
    fun build(
        prices: ItemPrices,
        options: PriceOptions,
    ): List<PriceLine> {
        val lines = ArrayList<PriceLine>(3)
        if (options.showBazaar) {
            prices.bazaarSell?.takeIf { it > 0 }?.let {
                lines += PriceLine(KEY_BAZAAR, listOf(amount(it, options.numberStyle)))
            }
        }
        if (options.showAuction) {
            prices.lowestBin?.takeIf { it > 0 }?.let {
                lines += PriceLine(KEY_LOWEST_BIN, listOf(amount(it, options.numberStyle)))
            }
        }
        if (options.showNpc) {
            prices.npcSell?.takeIf { it > 0 }?.let {
                lines += PriceLine(KEY_NPC, listOf(amount(it, options.numberStyle)))
            }
        }
        return lines
    }

    private fun amount(
        value: Double,
        style: NumberStyle,
    ): String =
        when (style) {
            NumberStyle.COMPACT -> Numbers.compact(value)
            NumberStyle.GROUPED -> Numbers.grouped(value.toLong())
        }

    private const val KEY_BAZAAR = "economy.price.bazaar"
    private const val KEY_LOWEST_BIN = "economy.price.lowest_bin"
    private const val KEY_NPC = "economy.price.npc"
}
