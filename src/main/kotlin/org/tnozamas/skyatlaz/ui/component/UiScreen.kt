package org.tnozamas.skyatlaz.ui.component

import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.input.CharacterEvent
import net.minecraft.client.input.KeyEvent
import net.minecraft.client.input.MouseButtonEvent
import org.tnozamas.skyatlaz.i18n.Tr
import org.tnozamas.skyatlaz.ui.theme.Theme

/**
 * Base for screens drawn with `ui.component`: dims the world, delegates input to a [UiRoot] and leaves the chrome
 * (panel, header, footer) to the subclass. The game keeps running; these are settings screens, not cutscenes.
 */
abstract class UiScreen(
    titleKey: String,
) : Screen(Tr.tr(titleKey)) {
    protected val root = UiRoot()

    final override fun isPauseScreen(): Boolean = false

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
        drawChrome(context, mouseX, mouseY)
        root.render(context, font, mouseX, mouseY)
    }

    /** Panel and static text. Called after the dim, before the widgets. */
    protected open fun drawChrome(
        context: GuiGraphicsExtractor,
        mouseX: Int,
        mouseY: Int,
    ) {}

    override fun mouseClicked(
        event: MouseButtonEvent,
        doubled: Boolean,
    ): Boolean = root.mouseClicked(event) || super.mouseClicked(event, doubled)

    override fun mouseDragged(
        event: MouseButtonEvent,
        dragX: Double,
        dragY: Double,
    ): Boolean = root.mouseDragged(event) || super.mouseDragged(event, dragX, dragY)

    override fun mouseReleased(event: MouseButtonEvent): Boolean = root.mouseReleased(event) || super.mouseReleased(event)

    override fun mouseScrolled(
        mouseX: Double,
        mouseY: Double,
        scrollX: Double,
        scrollY: Double,
    ): Boolean = root.mouseScrolled(mouseX, mouseY, scrollY) || super.mouseScrolled(mouseX, mouseY, scrollX, scrollY)

    override fun keyPressed(event: KeyEvent): Boolean = root.keyPressed(event) || super.keyPressed(event)

    override fun charTyped(event: CharacterEvent): Boolean = root.charTyped(event) || super.charTyped(event)
}
