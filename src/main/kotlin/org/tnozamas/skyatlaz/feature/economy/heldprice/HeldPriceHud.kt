package org.tnozamas.skyatlaz.feature.economy.heldprice

import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.network.chat.Component
import org.tnozamas.skyatlaz.i18n.Tr
import org.tnozamas.skyatlaz.ui.hud.HudElement
import org.tnozamas.skyatlaz.ui.theme.Spacing
import org.tnozamas.skyatlaz.ui.theme.Theme

/**
 * The card with the item in the player's hand and its price, as [HeldPriceLogic] decided.
 *
 * The content is rebuilt only when the item or the prices change, never per frame: the module hands over the rows
 * and the element translates and measures them when it is next drawn.
 */
class HeldPriceHud : HudElement("economy.heldprice", defaultX = 0.99, defaultY = 0.78) {
    private var content: HeldPriceContent = HeldPriceContent.EMPTY
    private var texts: List<Component> = emptyList()
    private var dirty = true

    /** Size of the card, including its padding. */
    override var width: Int = 0
        private set

    override var height: Int = 0
        private set

    /** New content to draw. Nothing happens when the card did not change. */
    fun update(content: HeldPriceContent) {
        if (this.content == content) return
        this.content = content
        dirty = true
    }

    override fun render(context: GuiGraphicsExtractor) {
        if (dirty) rebuild()
        if (texts.isEmpty()) return
        val font = Minecraft.getInstance().font
        context.fill(0, 0, width, height, Theme.cardPanel())
        context.outline(0, 0, width, height, Theme.cardBorder())
        var y = PADDING
        for (index in texts.indices) {
            val color = if (index == 0) Theme.TEXT else Theme.TEXT_MUTED
            context.text(font, texts[index], PADDING, y, Theme.argb(color), true)
            y += font.lineHeight + LINE_GAP
        }
    }

    /** Translates the rows and measures the card. The font only exists in game, so it happens on the first frame. */
    private fun rebuild() {
        dirty = false
        val font = Minecraft.getInstance().font
        val rows = ArrayList<Component>(content.rows.size + 1)
        if (content.title.isNotEmpty()) rows += Tr.verbatim(content.title)
        for (row in content.rows) rows += Tr.tr(row.key, *row.args.map { Tr.verbatim(it) }.toTypedArray())
        texts = rows
        val textWidth = texts.maxOfOrNull { font.width(it) } ?: 0
        val textHeight = if (texts.isEmpty()) 0 else texts.size * font.lineHeight + (texts.size - 1) * LINE_GAP
        width = textWidth + PADDING * 2
        height = textHeight + PADDING * 2
    }

    private companion object {
        const val PADDING = Spacing.PAD
        const val LINE_GAP = Spacing.GAP
    }
}
