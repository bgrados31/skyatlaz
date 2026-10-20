package org.tnozamas.skyatlaz.ui.component

import com.mojang.blaze3d.platform.InputConstants
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.Font
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.input.KeyEvent
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.network.chat.Component
import org.tnozamas.skyatlaz.core.keybind.KeyCombo
import org.tnozamas.skyatlaz.core.keybind.KeyModifiers
import org.tnozamas.skyatlaz.i18n.Tr
import org.tnozamas.skyatlaz.ui.render.KeyNames
import org.tnozamas.skyatlaz.ui.theme.Theme

/**
 * A row that shows the key of a binding and captures a new one when it is clicked.
 *
 * While the row listens, the next key press replaces the binding and Escape cancels it. The modifiers held at that
 * moment become part of the binding, so "Shift + F9" can be captured in one go.
 */
class UiKeyBind(
    private val labelKey: String,
    private val descriptionKey: String,
    private val get: () -> KeyCombo,
    private val onChange: (KeyCombo) -> Unit,
) : UiWidget() {
    override val focusable: Boolean get() = true

    override val searchText: String? get() = Tr.tr(labelKey).string

    private var listening = false

    override fun measure(font: Font): Int = UiMetrics.ROW_HEIGHT

    override fun render(
        ctx: GuiGraphicsExtractor,
        font: Font,
        mouseX: Int,
        mouseY: Int,
    ) {
        rowBackground(ctx, mouseX, mouseY)
        ctx.text(font, Tr.tr(labelKey), x + 4, labelY(font), Theme.argb(Theme.TEXT), false)
        val value = if (listening) Tr.tr("keybind.listening") else KeyNames.of(get())
        val color = if (listening) Theme.BRAND_AQUA else Theme.TEXT_MUTED
        ctx.text(font, value, x + width - 4 - font.width(value), labelY(font), Theme.argb(color), false)
    }

    override fun mouseClicked(event: MouseButtonEvent): Boolean {
        if (!contains(event.x().toInt(), event.y().toInt())) return false
        listening = !listening
        return true
    }

    override fun keyPressed(event: KeyEvent): Boolean {
        if (!listening) return false
        listening = false
        // Escape cancels instead of binding a key nobody can reach.
        if (event.key() == InputConstants.KEY_ESCAPE) return true
        val key = resolve(event) ?: return true
        onChange(KeyCombo(key.value, KeyModifiers.pressed(Minecraft.getInstance().window)))
        return true
    }

    /**
     * The key of the event as a key code, or null when it cannot be mapped. Some keys only report a scan code; its
     * name maps back to the key code we poll.
     */
    private fun resolve(event: KeyEvent): InputConstants.Key? {
        val key = InputConstants.getKey(event)
        if (key.type == InputConstants.Type.KEYSYM) return key.takeIf { it.value != InputConstants.UNKNOWN.value }
        val named = InputConstants.getKey(key.name)
        return named.takeIf { it.type == InputConstants.Type.KEYSYM && it.value != InputConstants.UNKNOWN.value }
    }

    override fun tooltip(
        mouseX: Int,
        mouseY: Int,
    ): Component? = if (contains(mouseX, mouseY)) Tr.tr(descriptionKey) else null
}
