package org.tnozamas.skyatlaz.feature.economy.heldprice

import net.minecraft.client.Minecraft
import net.minecraft.core.component.DataComponents
import net.minecraft.world.item.ItemStack
import org.tnozamas.skyatlaz.Skyatlaz
import org.tnozamas.skyatlaz.api.repo.PricesUpdatedEvent
import org.tnozamas.skyatlaz.core.module.FeatureCategory
import org.tnozamas.skyatlaz.core.module.Module
import org.tnozamas.skyatlaz.core.module.ModuleContext
import org.tnozamas.skyatlaz.util.ItemPrices
import org.tnozamas.skyatlaz.util.PriceOptions
import org.tnozamas.skyatlaz.util.SkyBlockItemData

/**
 * Shows the item in the player's hand and what it is worth, as a card on the HUD.
 *
 * Hypixel rules tier: Allowed. It shows prices that are public information, for the item the player is already
 * holding; it does not read the auction house of other players or anything the player cannot see.
 *
 * The card only exists while the player is on SkyBlock: the module stops (and its HUD element disappears) as soon as
 * the shared state says otherwise, so it costs nothing elsewhere. The prices are downloaded only while the feature
 * runs.
 */
object HeldPriceModule : Module("economy.heldprice", FeatureCategory.ECONOMY) {
    override val config: HeldPriceConfig = HeldPriceConfig

    /** Allowed and useful for everyone, so it starts on. */
    override val enabledByDefault: Boolean = true

    override fun isActive(): Boolean = Skyatlaz.SKYBLOCK.state.onSkyBlock

    override fun onEnable(ctx: ModuleContext) {
        val prices = ctx.usePrices()
        val hud = HeldPriceHud()
        ctx.hud(hud)

        // The item in hand changes when the player picks another one; the stack object changes with it, so one
        // reference comparison per tick is enough to notice.
        var held: ItemStack? = null
        val refresh = refresh@{
            val stack = Minecraft.getInstance().player?.mainHandItem
            val id = stack?.let { SkyBlockItemData.of(it).id }
            if (id == null) {
                hud.update(HeldPriceContent.EMPTY)
                return@refresh
            }
            val quote = prices.quote(id)
            hud.update(
                HeldPriceLogic.build(
                    name = stack.displayName(),
                    id = id,
                    prices =
                        ItemPrices(
                            bazaarSell = quote?.instantSell,
                            lowestBin = prices.lowestBins?.get(id),
                            npcSell = prices.item(id)?.npcSellPrice,
                        ),
                    options = options(),
                ),
            )
        }

        ctx.every(1) {
            val stack = Minecraft.getInstance().player?.mainHandItem
            if (stack !== held) {
                held = stack
                refresh()
            }
        }
        ctx.on<PricesUpdatedEvent> { refresh() }
        ctx.watch(HeldPriceConfig.showBazaar) { refresh() }
        ctx.watch(HeldPriceConfig.showAuction) { refresh() }
        ctx.watch(HeldPriceConfig.showNpc) { refresh() }
        ctx.watch(HeldPriceConfig.numberStyle) { refresh() }

        refresh()
    }

    private fun options(): PriceOptions =
        PriceOptions(
            showBazaar = HeldPriceConfig.showBazaar.value,
            showAuction = HeldPriceConfig.showAuction.value,
            showNpc = HeldPriceConfig.showNpc.value,
            numberStyle = HeldPriceConfig.numberStyle.value,
        )

    /** The name the player sees on the item, without its formatting. */
    private fun ItemStack.displayName(): String? = get(DataComponents.ITEM_NAME)?.string
}
