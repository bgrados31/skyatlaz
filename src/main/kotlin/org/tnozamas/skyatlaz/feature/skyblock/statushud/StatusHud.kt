package org.tnozamas.skyatlaz.feature.skyblock.statushud

import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.network.chat.Component
import org.tnozamas.skyatlaz.i18n.Tr
import org.tnozamas.skyatlaz.ui.hud.HudElement
import org.tnozamas.skyatlaz.ui.theme.Spacing
import org.tnozamas.skyatlaz.ui.theme.Theme

/**
 * The card with the player's SkyBlock status: area, profile, level and money, as [StatusHudLogic] decided.
 *
 * The content is rebuilt only when the state or the settings change, never per frame: the module hands over the
 * rows and the element translates and measures them when it is next drawn.
 */
class StatusHud : HudElement("skyblock.statushud", defaultX = 0.01, defaultY = 0.01) {
    private var lines: List<StatusHudLine> = emptyList()
    private var texts: List<Component> = emptyList()
    private var dirty = true

    /** Size of the card, including its padding. */
    override var width: Int = 0
        private set

    override var height: Int = 0
        private set

    /** New content to draw. Nothing happens when the rows did not change. */
    fun update(lines: List<StatusHudLine>) {
        if (this.lines == lines) return
        this.lines = lines
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
        texts = lines.map { line -> Tr.tr(line.key, *line.args.map { Tr.verbatim(it) }.toTypedArray()) }
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
