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

    /** A quiet horizontal line [pixels] wide. */
    fun rule(pixels: Int): MutableComponent = Tr.verbatim(spaces(pixels)).withStyle(RULE)

    /** Full-width line with [title] centered on it. */
    fun titledRule(title: Component): MutableComponent {
        val gap = width(Tr.verbatim(SPACE))
        val side = ((lineWidth() - width(title)) / 2 - gap).coerceAtLeast(gap * 2)
        return Tr
            .verbatim("")
            .append(rule(side))
            .append(SPACE)
            .append(title)
            .append(SPACE)
            .append(rule(side))
    }

    /** Spaces filling [pixels] (rounded up to whole spaces), for aligning columns. */
    fun pad(pixels: Int): MutableComponent = Tr.verbatim(spaces(pixels))

    private fun spaces(pixels: Int): String {
        val space =
            Minecraft
                .getInstance()
                .font
                .width(SPACE)
                .coerceAtLeast(1)
        return SPACE.repeat(((pixels + space - 1) / space).coerceAtLeast(0))
    }

    private val RULE: Style = Style.EMPTY.withColor(Theme.TEXT_SUBTLE).withStrikethrough(true)
}
