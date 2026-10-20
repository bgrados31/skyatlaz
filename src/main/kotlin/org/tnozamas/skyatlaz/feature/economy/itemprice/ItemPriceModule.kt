package org.tnozamas.skyatlaz.feature.economy.itemprice

import org.tnozamas.skyatlaz.SkyAtlaz
import org.tnozamas.skyatlaz.api.repo.PricesUpdatedEvent
import org.tnozamas.skyatlaz.core.config.GeneralConfig
import org.tnozamas.skyatlaz.core.module.FeatureCategory
import org.tnozamas.skyatlaz.core.module.Module
import org.tnozamas.skyatlaz.core.module.ModuleContext
import org.tnozamas.skyatlaz.core.tooltip.TooltipLines
import org.tnozamas.skyatlaz.i18n.Tr
import org.tnozamas.skyatlaz.ui.chat.Chat

/**
 * Shows the price of an item in its tooltip: the bazaar instant sell price, the lowest BIN and the NPC price.
 *
 * Hypixel rules tier: Allowed. It only shows prices that are public information; it does not read the auction house
 * of other players or anything the player cannot see.
 *
 * The prices are downloaded only while the feature runs, and only on SkyBlock, where the items live.
 */
object ItemPriceModule : Module("economy.itemprice", FeatureCategory.ECONOMY) {
    override val config: ItemPriceConfig = ItemPriceConfig

    /** Allowed and useful for everyone, so it starts on. */
    override val enabledByDefault: Boolean = true

    override fun isActive(): Boolean = SkyAtlaz.SKYBLOCK.state.onSkyBlock

    override fun onEnable(ctx: ModuleContext) {
        val provider = ItemPriceProvider(ctx.usePrices())
        TooltipLines.install(provider)
        ctx.track { TooltipLines.remove(provider) }
        var announced = false
        ctx.on<PricesUpdatedEvent> {
            provider.invalidate()
            // The prices take a moment to arrive; say so once, so an empty tooltip is not a mystery.
            if (!announced) {
                announced = true
                if (GeneralConfig.debugMode.value) Chat.info(Tr.tr("economy.itemprice.ready"))
            }
        }
        ctx.watch(ItemPriceConfig.showBazaar) { provider.invalidate() }
        ctx.watch(ItemPriceConfig.showAuction) { provider.invalidate() }
        ctx.watch(ItemPriceConfig.showNpc) { provider.invalidate() }
        ctx.watch(ItemPriceConfig.numberStyle) { provider.invalidate() }
    }
}
