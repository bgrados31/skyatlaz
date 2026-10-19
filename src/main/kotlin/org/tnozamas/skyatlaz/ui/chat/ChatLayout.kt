package org.tnozamas.skyatlaz.ui.chat

import net.minecraft.client.Minecraft
import net.minecraft.client.gui.components.ChatComponent
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.MutableComponent
import net.minecraft.network.chat.Style
import org.tnozamas.skyatlaz.i18n.Tr
import org.tnozamas.skyatlaz.ui.theme.Theme

/**
 * Layout primitives for chat panels, measured in real font pixels against the player's chat width and scale:
 * horizontal rules (struck-through spaces, the classic crisp chat line), centered titles and column padding.
 * Used when a panel is built (rare), never per frame.
 */
object ChatLayout {
    private const val SPACE = " "

    /** Usable width of one chat line in GUI pixels, following the chat width/scale options. */
    fun lineWidth(): Int {
        val options = Minecraft.getInstance().options
        val width = ChatComponent.getWidth(options.chatWidth().get()) / options.chatScale().get()
        return width.toInt() - 2
    }

    fun width(text: Component): Int = Minecraft.getInstance().font.width(text)

    /** A quiet horizontal line at most [pixels] wide (rounded down, so a full-width rule never wraps). */
    fun rule(pixels: Int): MutableComponent = Tr.verbatim(spaces(pixels, roundUp = false)).withStyle(RULE)

    /** Full-width line with [title] centered on it, with [TITLE_GAP] pixels of air on each side of the title. */
    fun titledRule(title: Component): MutableComponent {
        val gap = pad(TITLE_GAP)
        val side = ((lineWidth() - width(title)) / 2 - width(gap)).coerceAtLeast(TITLE_GAP)
        return Tr
            .verbatim("")
            .append(rule(side))
            .append(gap.copy())
            .append(title)
            .append(gap)
            .append(rule(side))
    }

    private const val TITLE_GAP = 8

    /** Spaces filling [pixels] (rounded up to whole spaces), for aligning columns. */
    fun pad(pixels: Int): MutableComponent = Tr.verbatim(spaces(pixels, roundUp = true))

    private fun spaces(
        pixels: Int,
        roundUp: Boolean,
    ): String {
        val space =
            Minecraft
                .getInstance()
                .font
                .width(SPACE)
                .coerceAtLeast(1)
        val count = if (roundUp) (pixels + space - 1) / space else pixels / space
        return SPACE.repeat(count.coerceAtLeast(0))
    }

    private val RULE: Style = Style.EMPTY.withColor(Theme.TEXT_SUBTLE).withStrikethrough(true)
}
