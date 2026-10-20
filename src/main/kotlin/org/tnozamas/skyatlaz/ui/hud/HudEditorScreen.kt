package org.tnozamas.skyatlaz.ui.hud

import com.mojang.blaze3d.platform.InputConstants
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.input.KeyEvent
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.network.chat.Component
import org.tnozamas.skyatlaz.i18n.Tr
import org.tnozamas.skyatlaz.i18n.withGradient
import org.tnozamas.skyatlaz.ui.theme.Theme
import java.util.Locale

/**
 * In-game layout editor (`/skyatlaz hud`): drag an element to move it, scroll over it to scale it, `R` to reset it,
 * `Tab` to cycle the selection and `Esc` to close. The normal HUD is hidden while this screen is open; the editor
 * draws the same elements itself so they can be outlined and selected.
 *
 * Positions are saved through [HudPlacement] as soon as they change, so closing the game right after a drag keeps
 * the layout. The game keeps running behind the screen ([isPauseScreen] is false).
 */
class HudEditorScreen(
    private val hud: HudManager,
) : Screen(Tr.tr("hud.editor.title")) {
    private val heading = Tr.tr("hud.editor.title").withGradient(Theme.BRAND_GRADIENT)
    private val hint = Tr.tr("hud.editor.hint")
    private val empty = Tr.tr("hud.editor.empty")

    private var selected: HudElement? = null
    private var dragging: HudElement? = null
    private var hovered: HudElement? = null
    private var grabX = 0.0
    private var grabY = 0.0
    private var info: Component? = null

    init {
        select(hud.activeElements.firstOrNull())
    }

    override fun isPauseScreen(): Boolean = false

    override fun extractBackground(
        context: GuiGraphicsExtractor,
        mouseX: Int,
        mouseY: Int,
        delta: Float,
    ) {
        context.fill(0, 0, width, height, Theme.argb(Theme.SCREEN_DIM, 168))
    }

    override fun extractRenderState(
        context: GuiGraphicsExtractor,
        mouseX: Int,
        mouseY: Int,
        delta: Float,
    ) {
        super.extractRenderState(context, mouseX, mouseY, delta)
        hovered = elementAt(mouseX.toDouble(), mouseY.toDouble())

        context.centeredText(font, heading, width / 2, 12, Theme.argb(Theme.TEXT))

        val elements = hud.activeElements
        if (elements.isEmpty()) {
            context.centeredText(font, empty, width / 2, height / 2, Theme.argb(Theme.TEXT_MUTED))
        } else {
            for (element in elements) drawElement(context, element)
            info?.let { context.centeredText(font, it, width / 2, height - 74, Theme.argb(Theme.TEXT_MUTED)) }
        }
        context.centeredText(font, hint, width / 2, height - 54, Theme.argb(Theme.TEXT_SUBTLE))
    }

    override fun mouseClicked(
        event: MouseButtonEvent,
        doubled: Boolean,
    ): Boolean {
        if (event.button() != 0) return super.mouseClicked(event, doubled)
        val hit =
            elementAt(event.x(), event.y()) ?: run {
                select(null)
                return false
            }
        select(hit)
        dragging = hit
        grabX = event.x() - hit.layout.resolveX(width, hit.width)
        grabY = event.y() - hit.layout.resolveY(height, hit.height)
        return true
    }

    override fun mouseDragged(
        event: MouseButtonEvent,
        dragX: Double,
        dragY: Double,
    ): Boolean {
        val element = dragging ?: return super.mouseDragged(event, dragX, dragY)
        val placement = element.layout
        placement.moveTo(
            fraction(event.x() - grabX, width, placement.scaledWidth(element.width)),
            fraction(event.y() - grabY, height, placement.scaledHeight(element.height)),
        )
        refreshInfo()
        return true
    }

    override fun mouseReleased(event: MouseButtonEvent): Boolean {
        if (dragging == null) return super.mouseReleased(event)
        dragging = null
        return true
    }

    override fun mouseScrolled(
        mouseX: Double,
        mouseY: Double,
        scrollX: Double,
        scrollY: Double,
    ): Boolean {
        val element = elementAt(mouseX, mouseY) ?: return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY)
        if (scrollY == 0.0) return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY)
        select(element)
        element.layout.setScale(element.layout.scale + scrollY * SCALE_STEP)
        refreshInfo()
        return true
    }

    override fun keyPressed(event: KeyEvent): Boolean {
        val current = selected
        if (event.key() == InputConstants.KEY_R && current != null) {
            current.layout.reset()
            refreshInfo()
            return true
        }
        if (event.key() == InputConstants.KEY_TAB) {
            cycleSelection()
            return true
        }
        return super.keyPressed(event)
    }

    private fun drawElement(
        context: GuiGraphicsExtractor,
        element: HudElement,
    ) {
        val placement = element.layout
        val x = placement.resolveX(width, element.width)
        val y = placement.resolveY(height, element.height)
        val scaledWidth = placement.scaledWidth(element.width)
        val scaledHeight = placement.scaledHeight(element.height)

        hud.draw(context, element)
        val color =
            when (element) {
                selected -> Theme.BRAND_AQUA
                hovered -> Theme.BRAND_LAVENDER
                else -> Theme.TEXT_SUBTLE
            }
        context.outline(x - 1, y - 1, scaledWidth + 2, scaledHeight + 2, Theme.argb(color))
        val labelY = if (y >= font.lineHeight + 4) y - font.lineHeight - 2 else y + scaledHeight + 2
        context.text(font, element.name, x, labelY, Theme.argb(color), true)
    }

    private fun cycleSelection() {
        val elements = hud.activeElements
        if (elements.isEmpty()) {
            select(null)
            return
        }
        val index = elements.indexOf(selected)
        select(elements[(index + 1) % elements.size])
    }

    private fun select(element: HudElement?) {
        selected = element
        refreshInfo()
    }

    private fun refreshInfo() {
        info =
            selected?.let { element ->
                val placement = element.layout
                Tr.tr(
                    "hud.editor.info",
                    Tr.verbatim(decimal(placement.x)),
                    Tr.verbatim(decimal(placement.y)),
                    Tr.verbatim(decimal(placement.scale)),
                )
            }
    }

    private fun elementAt(
        mouseX: Double,
        mouseY: Double,
    ): HudElement? {
        val elements = hud.activeElements
        for (index in elements.indices.reversed()) {
            val element = elements[index]
            val placement = element.layout
            val x = placement.resolveX(width, element.width)
            val y = placement.resolveY(height, element.height)
            if (mouseX >= x &&
                mouseY >= y &&
                mouseX < x + placement.scaledWidth(element.width) &&
                mouseY < y + placement.scaledHeight(element.height)
            ) {
                return element
            }
        }
        return null
    }

    private companion object {
        const val SCALE_STEP = 0.05

        fun fraction(
            pixel: Double,
            screenSize: Int,
            scaledSize: Int,
        ): Double {
            val available = screenSize - scaledSize
            return if (available <= 0) 0.0 else (pixel / available).coerceIn(0.0, 1.0)
        }

        fun decimal(value: Double): String = String.format(Locale.ROOT, "%.2f", value)
    }
}
