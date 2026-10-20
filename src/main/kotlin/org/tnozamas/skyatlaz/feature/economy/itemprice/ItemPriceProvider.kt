package org.tnozamas.skyatlaz.feature.economy.itemprice

import net.minecraft.network.chat.Component
import net.minecraft.world.item.ItemStack
import org.tnozamas.skyatlaz.api.repo.PriceRepository
import org.tnozamas.skyatlaz.core.tooltip.TooltipLines
import org.tnozamas.skyatlaz.i18n.Tr
import org.tnozamas.skyatlaz.util.ItemPrices
import org.tnozamas.skyatlaz.util.PriceLines
import org.tnozamas.skyatlaz.util.PriceOptions
import org.tnozamas.skyatlaz.util.SkyBlockItemData

/**
 * Adds the price lines of an item to its tooltip.
 *
 * The vanilla tooltip is rebuilt every frame, so this reads the SkyBlock data of the item only once (the same stack
 * object comes back every frame) and keeps the built lines until the prices or the settings change.
 */
class ItemPriceProvider(
    private val prices: PriceRepository,
) : TooltipLines.Provider {
    private var version = 0
    private var lastStack: ItemStack? = null
    private var lastData: SkyBlockItemData = SkyBlockItemData.EMPTY
    private var cachedId: String? = null
    private var cachedVersion = -1
    private var cachedLines: List<Component> = emptyList()

    /** The prices or the settings changed: the next tooltip is built again. */
    fun invalidate() {
        version++
    }

    override fun lines(stack: ItemStack): List<Component> {
        val id = dataOf(stack).id ?: return emptyList()
        if (id == cachedId && version == cachedVersion) return cachedLines
        val built = build(id)
        cachedId = id
        cachedVersion = version
        cachedLines = built
        return built
    }

    private fun build(id: String): List<Component> {
        val quote = prices.quote(id)
        val catalog = prices.item(id)
        val lines =
            PriceLines.build(
                ItemPrices(
                    bazaarSell = quote?.instantSell,
                    lowestBin = prices.lowestBins?.get(id),
                    npcSell = catalog?.npcSellPrice,
                ),
                options(),
            )
        return lines.map { line -> Tr.tr(line.key, *line.args.map { Tr.verbatim(it) }.toTypedArray()) }
    }

    private fun options(): PriceOptions =
        PriceOptions(
            showBazaar = ItemPriceConfig.showBazaar.value,
            showAuction = ItemPriceConfig.showAuction.value,
            showNpc = ItemPriceConfig.showNpc.value,
            numberStyle = ItemPriceConfig.numberStyle.value,
        )

    /** The SkyBlock data of the item. Read once per item: the tooltip is rebuilt every frame. */
    private fun dataOf(stack: ItemStack): SkyBlockItemData {
        if (lastStack === stack) return lastData
        lastStack = stack
        lastData = SkyBlockItemData.of(stack)
        return lastData
    }
}
