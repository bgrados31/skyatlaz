package org.tnozamas.skyatlaz.feature.economy.itemprice

import org.tnozamas.skyatlaz.core.config.ConfigSection
import org.tnozamas.skyatlaz.util.NumberStyle

/**
 * Settings of the item price tooltip. The on/off switch of the feature is the module's own toggle
 * (`module.economy.itemprice.enabled`); these entries decide which prices are shown and how they are written.
 */
object ItemPriceConfig : ConfigSection("economy.itemprice") {
    /** The bazaar instant sell price: what the player gets when selling right now. */
    val showBazaar = boolean("show_bazaar", true)

    /** The lowest buy-it-now price on the auction house (only when a provider is enabled). */
    val showAuction = boolean("show_auction", true)

    /** What the NPC shops pay for the item. */
    val showNpc = boolean("show_npc", false)

    val numberStyle = choice("number_style", NumberStyle.COMPACT)
}
