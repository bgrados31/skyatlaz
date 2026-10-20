package org.tnozamas.skyatlaz.ui.component

import net.minecraft.client.gui.Font
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.input.CharacterEvent
import net.minecraft.client.input.KeyEvent
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.network.chat.Component
import org.tnozamas.skyatlaz.ui.theme.Spacing
import org.tnozamas.skyatlaz.ui.theme.Theme

/** Shared spacing and sizing for `ui.component`. Keeps screens on the same rhythm instead of per-screen numbers. */
object UiMetrics {
    const val ROW_HEIGHT = 26
    const val ROW_GAP = Spacing.GAP
    const val SECTION_HEIGHT = 30
    const val FIELD_HEIGHT = 20
    const val BUTTON_HEIGHT = 22
    const val TAB_HEIGHT = 26
    const val TAB_PADDING = 18
    const val LIST_PADDING = Spacing.PAD_WIDE
    const val SCROLLBAR_WIDTH = 2
}

/**
 * Base class of the mod's own widget kit. Widgets are retained objects laid out in absolute screen coordinates each
 * frame (no transform stack), so hit testing and rendering always agree, and an ancestor can clip with a scissor
 * without the children knowing.
 *
 * A widget only handles what it owns: [UiList] forwards mouse events and the screen forwards keyboard events to the
 * focused widget. See `docs/ARCHITECTURE.md` for the division between `ui.component`, `ui.hud` and features.
 */
abstract class UiWidget {
    var x = 0
    var y = 0
    var width = 0
    var height = 0

    open val focusable: Boolean get() = false
    var focused = false

    open fun measure(font: Font): Int = height

    open fun layout(
        x: Int,
        y: Int,
        width: Int,
        font: Font,
    ) {
        this.x = x
        this.y = y
        this.width = width
        height = measure(font)
    }

    open fun render(
        ctx: GuiGraphicsExtractor,
        font: Font,
        mouseX: Int,
        mouseY: Int,
    ) {}

    open fun mouseClicked(event: MouseButtonEvent): Boolean = false

    open fun mouseDragged(event: MouseButtonEvent): Boolean = false

    open fun mouseReleased(event: MouseButtonEvent): Boolean = false

    open fun mouseScrolled(
        mouseX: Double,
        mouseY: Double,
        amount: Double,
    ): Boolean = false

    open fun keyPressed(event: KeyEvent): Boolean = false

    open fun charTyped(event: CharacterEvent): Boolean = false

    /** Extra text shown while the pointer rests on this widget, or null. */
    open fun tooltip(
        mouseX: Int,
        mouseY: Int,
    ): Component? = null

    /** The text the settings search looks at, or null when the row has no text of its own. */
    open val searchText: String? get() = null

    fun contains(
        mouseX: Int,
        mouseY: Int,
    ): Boolean = mouseX >= x && mouseY >= y && mouseX < x + width && mouseY < y + height

    protected fun rowBackground(
        ctx: GuiGraphicsExtractor,
        mouseX: Int,
        mouseY: Int,
    ) {
        val hovered = contains(mouseX, mouseY)
        if (hovered || focused) {
            ctx.fill(
                x - 6,
                y,
                x + width,
                y + height,
                Theme.argb(if (hovered) Theme.SURFACE_RAISED else Theme.SURFACE, if (hovered) 190 else 120),
            )
        }
        if (focused) ctx.outline(x - 7, y - 1, width + 8, height + 2, Theme.argb(Theme.BRAND_AQUA, 120))
    }

    protected fun labelY(font: Font): Int = y + (height - font.lineHeight) / 2
}
