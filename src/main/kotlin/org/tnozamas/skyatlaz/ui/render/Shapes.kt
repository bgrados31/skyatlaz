package org.tnozamas.skyatlaz.ui.render

import net.minecraft.client.gui.Font
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.network.chat.Component
import org.tnozamas.skyatlaz.ui.theme.Theme

/**
 * The drawing primitives every SkyAtlaz surface shares: HUD elements, chat and screens.
 *
 * They take a theme token instead of a raw color and build the ARGB value here, so a feature never repeats
 * `Theme.argb(...)` and every surface keeps the same look. Nothing here allocates: the methods only forward to the
 * vanilla drawing calls.
 */
object Shapes {
    /** A solid rectangle. */
    fun fill(
        context: GuiGraphicsExtractor,
        x: Int,
        y: Int,
        width: Int,
        height: Int,
        rgb: Int,
        alpha: Int = 255,
    ) {
        if (width <= 0 || height <= 0) return
        context.fill(x, y, x + width, y + height, Theme.argb(rgb, alpha))
    }

    /** A one pixel border around a rectangle. */
    fun outline(
        context: GuiGraphicsExtractor,
        x: Int,
        y: Int,
        width: Int,
        height: Int,
        rgb: Int,
        alpha: Int = 255,
    ) {
        if (width <= 0 || height <= 0) return
        context.outline(x, y, width, height, Theme.argb(rgb, alpha))
    }

    /** The standard soft surface: a translucent panel with a quiet border. */
    fun panel(
        context: GuiGraphicsExtractor,
        x: Int,
        y: Int,
        width: Int,
        height: Int,
        alpha: Int = 248,
    ) {
        fill(context, x, y, width, height, Theme.SURFACE, alpha)
        outline(context, x, y, width, height, Theme.BORDER, 220)
    }

    /** Text at a position, in a theme color. */
    fun text(
        context: GuiGraphicsExtractor,
        font: Font,
        text: Component,
        x: Int,
        y: Int,
        rgb: Int,
        shadow: Boolean = true,
    ) {
        context.text(font, text, x, y, Theme.argb(rgb), shadow)
    }

    /** Text whose right edge is at [right], so it can be aligned to the other side of a surface. */
    fun textRight(
        context: GuiGraphicsExtractor,
        font: Font,
        text: Component,
        right: Int,
        y: Int,
        rgb: Int,
        shadow: Boolean = true,
    ) {
        context.text(font, text, right - font.width(text), y, Theme.argb(rgb), shadow)
    }

    /** Text centered on [centerX]. */
    fun textCentered(
        context: GuiGraphicsExtractor,
        font: Font,
        text: Component,
        centerX: Int,
        y: Int,
        rgb: Int,
        shadow: Boolean = true,
    ) {
        context.text(font, text, centerX - font.width(text) / 2, y, Theme.argb(rgb), shadow)
    }
}
