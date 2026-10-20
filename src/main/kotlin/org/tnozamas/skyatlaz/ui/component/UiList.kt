package org.tnozamas.skyatlaz.ui.component

import net.minecraft.client.gui.Font
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.network.chat.Component
import org.tnozamas.skyatlaz.ui.theme.Theme
import kotlin.math.max

/**
 * Vertical list with a scissor, a proportional scrollbar and drag forwarding. Children are laid out in absolute
 * coordinates every frame from the current scroll offset, so nothing has to translate the matrix stack.
 */
class UiList : UiWidget() {
    private val children = ArrayList<UiWidget>()
    private var contentHeight = 0
    private var scrollPixels = 0.0
    private var dragTarget: UiWidget? = null

    val widgets: List<UiWidget> get() = children

    fun setWidgets(widgets: List<UiWidget>) {
        children.clear()
        children.addAll(widgets)
        scrollPixels = scrollPixels.coerceIn(0.0, maxScroll().toDouble())
    }

    /** Returns to the first row, e.g. after changing tab or filtering. */
    fun scrollToTop() {
        scrollPixels = 0.0
    }

    fun focusables(): List<UiWidget> = children.filter { it.focusable }

    override fun layout(
        x: Int,
        y: Int,
        width: Int,
        font: Font,
    ) {
        super.layout(x, y, width, font)
        arrange(font)
    }

    /** The list viewport has an explicit height, unlike content-sized rows. */
    fun layout(
        x: Int,
        y: Int,
        width: Int,
        height: Int,
        font: Font,
    ) {
        this.x = x
        this.y = y
        this.width = width
        this.height = height
        arrange(font)
    }

    override fun render(
        ctx: GuiGraphicsExtractor,
        font: Font,
        mouseX: Int,
        mouseY: Int,
    ) {
        arrange(font)
        revealFocused(font)
        ctx.enableScissor(x, y, x + width, y + height)
        for (child in children) {
            if (child.y + child.height >= y && child.y <= y + height) child.render(ctx, font, mouseX, mouseY)
        }
        ctx.disableScissor()
        drawScrollbar(ctx)
    }

    override fun mouseClicked(event: MouseButtonEvent): Boolean {
        if (!contains(event.x().toInt(), event.y().toInt())) return false
        val target = childAt(event.x().toInt(), event.y().toInt()) ?: return false
        dragTarget = target
        return target.mouseClicked(event)
    }

    override fun mouseDragged(event: MouseButtonEvent): Boolean = dragTarget?.mouseDragged(event) ?: false

    override fun mouseReleased(event: MouseButtonEvent): Boolean {
        val target = dragTarget ?: return false
        dragTarget = null
        return target.mouseReleased(event)
    }

    override fun mouseScrolled(
        mouseX: Double,
        mouseY: Double,
        amount: Double,
    ): Boolean {
        if (!contains(mouseX.toInt(), mouseY.toInt()) || amount == 0.0) return false
        scrollPixels = (scrollPixels - amount * SCROLL_STEP).coerceIn(0.0, maxScroll().toDouble())
        return true
    }

    override fun tooltip(
        mouseX: Int,
        mouseY: Int,
    ): Component? {
        if (!contains(mouseX, mouseY)) return null
        return childAt(mouseX, mouseY)?.tooltip(mouseX, mouseY)
    }

    /** Deepest child under the pointer, or null when the list shows empty space. */
    fun childAt(
        mouseX: Int,
        mouseY: Int,
    ): UiWidget? {
        if (!contains(mouseX, mouseY)) return null
        for (index in children.indices.reversed()) {
            val child = children[index]
            if (child.contains(mouseX, mouseY)) return child
        }
        return null
    }

    private fun arrange(font: Font) {
        var cursor = UiMetrics.LIST_PADDING
        for (child in children) {
            child.layout(x + 2, y + cursor - scrollPixels.toInt(), width - 8, font)
            cursor += child.height + UiMetrics.ROW_GAP
        }
        contentHeight = cursor + UiMetrics.LIST_PADDING
    }

    private fun maxScroll(): Int = max(0, contentHeight - height)

    /** Keyboard focus can move to a row outside the viewport; scroll just enough to show it. */
    private fun revealFocused(font: Font) {
        for (child in children) {
            if (child.focused) {
                reveal(child, font)
                return
            }
        }
    }

    /** Scrolls just enough to show [child], e.g. the selected row of a palette. */
    fun reveal(
        child: UiWidget,
        font: Font,
    ) {
        val top = y + UiMetrics.LIST_PADDING
        val bottom = y + height - UiMetrics.LIST_PADDING
        val delta =
            when {
                child.y < top -> child.y - top
                child.y + child.height > bottom -> child.y + child.height - bottom
                else -> return
            }
        scrollPixels = (scrollPixels + delta).coerceIn(0.0, maxScroll().toDouble())
        arrange(font)
    }

    private fun drawScrollbar(ctx: GuiGraphicsExtractor) {
        val max = maxScroll()
        if (max <= 0) return
        val trackHeight = height - 2 * UiMetrics.LIST_PADDING
        val thumbHeight = max(12, trackHeight * height / contentHeight)
        val thumbY =
            y + UiMetrics.LIST_PADDING +
                ((trackHeight - thumbHeight) * (scrollPixels / max)).toInt()
        val barX = x + width - UiMetrics.SCROLLBAR_WIDTH
        ctx.fill(
            barX,
            y + UiMetrics.LIST_PADDING,
            barX + UiMetrics.SCROLLBAR_WIDTH,
            y + height - UiMetrics.LIST_PADDING,
            Theme.argb(Theme.BORDER, 90),
        )
        ctx.fill(barX, thumbY, barX + UiMetrics.SCROLLBAR_WIDTH, thumbY + thumbHeight, Theme.argb(Theme.BRAND_LAVENDER, 170))
    }

    private companion object {
        const val SCROLL_STEP = 22.0
    }
}
