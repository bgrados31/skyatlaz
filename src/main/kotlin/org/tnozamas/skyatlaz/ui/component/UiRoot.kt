package org.tnozamas.skyatlaz.ui.component

import com.mojang.blaze3d.platform.InputConstants
import net.minecraft.client.gui.Font
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.input.CharacterEvent
import net.minecraft.client.input.KeyEvent
import net.minecraft.client.input.MouseButtonEvent

/**
 * Focus, hit testing and event routing for a flat set of widgets. Screens own the layout; the root owns the pointer
 * and keyboard policy: click focuses, Tab cycles, arrows move between rows, and a focused widget gets the key first.
 */
class UiRoot {
    private val widgets = ArrayList<UiWidget>()

    fun setWidgets(widgets: List<UiWidget>) {
        this.widgets.clear()
        this.widgets.addAll(widgets)
        val current = focused
        if (current != null && current !in focusCandidates()) focus(null)
    }

    fun render(
        ctx: GuiGraphicsExtractor,
        font: Font,
        mouseX: Int,
        mouseY: Int,
    ) {
        for (widget in widgets) widget.render(ctx, font, mouseX, mouseY)
        val tooltip = hitTest(mouseX, mouseY)?.tooltip(mouseX, mouseY) ?: return
        ctx.setTooltipForNextFrame(font, tooltip, mouseX, mouseY)
    }

    fun mouseClicked(event: MouseButtonEvent): Boolean {
        val mouseX = event.x().toInt()
        val mouseY = event.y().toInt()
        for (widget in widgets.asReversed()) {
            if (widget is UiList) {
                if (!widget.contains(mouseX, mouseY)) continue
                focus(widget.childAt(mouseX, mouseY)?.takeIf { it.focusable })
                return widget.mouseClicked(event)
            }
            if (widget.contains(mouseX, mouseY)) {
                focus(widget.takeIf { it.focusable })
                return widget.mouseClicked(event)
            }
        }
        focus(null)
        return false
    }

    fun mouseDragged(event: MouseButtonEvent): Boolean {
        var handled = false
        for (widget in widgets) if (widget.mouseDragged(event)) handled = true
        return handled
    }

    fun mouseReleased(event: MouseButtonEvent): Boolean {
        var handled = false
        for (widget in widgets) if (widget.mouseReleased(event)) handled = true
        return handled
    }

    fun mouseScrolled(
        mouseX: Double,
        mouseY: Double,
        amount: Double,
    ): Boolean {
        for (widget in widgets.asReversed()) {
            if (widget.mouseScrolled(mouseX, mouseY, amount)) return true
        }
        return false
    }

    fun keyPressed(event: KeyEvent): Boolean {
        if (event.isCycleFocus()) {
            moveFocus(if (event.hasShiftDown()) -1 else 1)
            return true
        }
        val current = focused
        if (current != null && current.keyPressed(event)) return true
        return when (event.key()) {
            InputConstants.KEY_DOWN -> moveFocus(1)
            InputConstants.KEY_UP -> moveFocus(-1)
            else -> false
        }
    }

    fun charTyped(event: CharacterEvent): Boolean = focused?.charTyped(event) ?: false

    fun focus(widget: UiWidget?) {
        if (focused === widget) return
        focused?.focused = false
        focused = widget?.takeIf { it.focusable }
        focused?.focused = true
    }

    private fun moveFocus(direction: Int): Boolean {
        val candidates = focusCandidates()
        if (candidates.isEmpty()) return false
        val current = focused
        val index = if (current == null) -1 else candidates.indexOf(current)
        val next = Math.floorMod(if (index < 0) (if (direction > 0) 0 else candidates.lastIndex) else index + direction, candidates.size)
        focus(candidates[next])
        return true
    }

    private fun focusCandidates(): List<UiWidget> =
        widgets.flatMap { if (it is UiList) it.focusables() else listOf(it) }.filter { it.focusable }

    /** Deepest interactive target under the pointer; list children win over the list itself. */
    private fun hitTest(
        mouseX: Int,
        mouseY: Int,
    ): UiWidget? {
        for (widget in widgets.asReversed()) {
            if (widget is UiList) {
                val child = widget.childAt(mouseX, mouseY)
                if (child != null) return child
                if (widget.contains(mouseX, mouseY)) return widget
            } else if (widget.contains(mouseX, mouseY)) {
                return widget
            }
        }
        return null
    }

    private var focused: UiWidget? = null
}
