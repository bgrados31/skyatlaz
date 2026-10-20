package org.tnozamas.skyatlaz.ui.component

import com.mojang.blaze3d.platform.InputConstants
import net.minecraft.client.gui.Font
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.input.CharacterEvent
import net.minecraft.client.input.KeyEvent
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.network.chat.Component
import org.tnozamas.skyatlaz.i18n.Tr
import org.tnozamas.skyatlaz.ui.theme.Theme
import kotlin.math.roundToInt

/** Section heading inside a [UiList]: muted label and a hairline that fills the remaining width. */
class UiSection(
    private val titleKey: String,
) : UiWidget() {
    override fun measure(font: Font): Int = UiMetrics.SECTION_HEIGHT

    override fun render(
        ctx: GuiGraphicsExtractor,
        font: Font,
        mouseX: Int,
        mouseY: Int,
    ) {
        val label = Tr.tr(titleKey)
        val textY = y + 10
        ctx.text(font, label, x + 4, textY, Theme.argb(Theme.TEXT_MUTED), false)
        val lineX = x + 4 + font.width(label) + 10
        if (lineX < x + width - 4) {
            val lineY = textY + font.lineHeight / 2
            ctx.fill(lineX, lineY, x + width - 4, lineY + 1, Theme.argb(Theme.BORDER, 150))
        }
    }
}

/** Quiet line of text, used for "no results" and other list-level notes. */
class UiNote(
    private val textKey: String,
) : UiWidget() {
    override fun measure(font: Font): Int = UiMetrics.ROW_HEIGHT

    override fun render(
        ctx: GuiGraphicsExtractor,
        font: Font,
        mouseX: Int,
        mouseY: Int,
    ) {
        ctx.text(font, Tr.tr(textKey), x + 4, labelY(font), Theme.argb(Theme.TEXT_SUBTLE), false)
    }
}

/** Boolean row: label on the left, a small track and knob on the right. */
class UiToggle(
    private val labelKey: String,
    private val descriptionKey: String,
    private val get: () -> Boolean,
    private val set: (Boolean) -> Unit,
) : UiWidget() {
    override val focusable: Boolean get() = true

    override fun measure(font: Font): Int = UiMetrics.ROW_HEIGHT

    override fun render(
        ctx: GuiGraphicsExtractor,
        font: Font,
        mouseX: Int,
        mouseY: Int,
    ) {
        rowBackground(ctx, mouseX, mouseY)
        ctx.text(font, Tr.tr(labelKey), x + 4, labelY(font), Theme.argb(Theme.TEXT), false)

        val on = get()
        val trackWidth = 28
        val trackHeight = 12
        val trackX = x + width - trackWidth - 4
        val trackY = y + (height - trackHeight) / 2
        ctx.fill(
            trackX,
            trackY,
            trackX + trackWidth,
            trackY + trackHeight,
            Theme.argb(if (on) Theme.BRAND_AQUA else Theme.TRACK, if (on) 190 else 255),
        )
        val knobWidth = 10
        val knobX = if (on) trackX + trackWidth - knobWidth - 1 else trackX + 1
        ctx.fill(
            knobX,
            trackY + 1,
            knobX + knobWidth,
            trackY + trackHeight - 1,
            Theme.argb(if (on) Theme.SURFACE_SUNKEN else Theme.TEXT_MUTED),
        )
    }

    override fun mouseClicked(event: MouseButtonEvent): Boolean {
        if (!contains(event.x().toInt(), event.y().toInt())) return false
        set(!get())
        return true
    }

    override fun keyPressed(event: KeyEvent): Boolean {
        if (event.key() != InputConstants.KEY_RETURN && event.key() != InputConstants.KEY_SPACE) return false
        set(!get())
        return true
    }

    override fun tooltip(
        mouseX: Int,
        mouseY: Int,
    ): Component? = if (contains(mouseX, mouseY)) Tr.tr(descriptionKey) else null
}

/** Integer slider with the value on the right and a thin track under the label. */
class UiSlider(
    private val labelKey: String,
    private val descriptionKey: String,
    private val min: Int,
    private val max: Int,
    private val step: Int,
    private val get: () -> Int,
    private val set: (Int) -> Unit,
    private val format: (Int) -> Component,
) : UiWidget() {
    private var dragging = false

    override val focusable: Boolean get() = true

    override fun measure(font: Font): Int = 38

    override fun render(
        ctx: GuiGraphicsExtractor,
        font: Font,
        mouseX: Int,
        mouseY: Int,
    ) {
        rowBackground(ctx, mouseX, mouseY)
        ctx.text(font, Tr.tr(labelKey), x + 4, y + 3, Theme.argb(Theme.TEXT), false)
        val value = format(get())
        ctx.text(font, value, x + width - 4 - font.width(value), y + 3, Theme.argb(Theme.TEXT_MUTED), false)

        val trackX = x + 4
        val trackWidth = width - 8
        val trackY = y + 28
        ctx.fill(trackX, trackY, trackX + trackWidth, trackY + 3, Theme.argb(Theme.TRACK))
        val fraction = fraction()
        val handleX = trackX + (fraction * (trackWidth - 2)).roundToInt()
        ctx.fill(trackX, trackY, handleX, trackY + 3, Theme.argb(Theme.BRAND_AQUA, 200))
        ctx.fill(handleX, trackY - 3, handleX + 2, trackY + 6, Theme.argb(if (dragging) Theme.BRAND_LAVENDER else Theme.TEXT))
    }

    override fun mouseClicked(event: MouseButtonEvent): Boolean {
        if (!contains(event.x().toInt(), event.y().toInt())) return false
        dragging = true
        setFromMouse(event.x())
        return true
    }

    override fun mouseDragged(event: MouseButtonEvent): Boolean {
        if (!dragging) return false
        setFromMouse(event.x())
        return true
    }

    override fun mouseReleased(event: MouseButtonEvent): Boolean {
        val wasDragging = dragging
        dragging = false
        return wasDragging
    }

    override fun mouseScrolled(
        mouseX: Double,
        mouseY: Double,
        amount: Double,
    ): Boolean {
        if (!contains(mouseX.toInt(), mouseY.toInt()) || amount == 0.0) return false
        set((get() + if (amount > 0) step else -step).coerceIn(min, max))
        return true
    }

    override fun keyPressed(event: KeyEvent): Boolean {
        val next =
            when (event.key()) {
                InputConstants.KEY_LEFT -> get() - step
                InputConstants.KEY_RIGHT -> get() + step
                InputConstants.KEY_HOME -> min
                InputConstants.KEY_END -> max
                else -> return false
            }
        set(next.coerceIn(min, max))
        return true
    }

    override fun tooltip(
        mouseX: Int,
        mouseY: Int,
    ): Component? = if (contains(mouseX, mouseY)) Tr.tr(descriptionKey) else null

    private fun setFromMouse(mouseX: Double) {
        if (max <= min) return
        val trackWidth = width - 8
        val fraction = ((mouseX - x - 4) / trackWidth).coerceIn(0.0, 1.0)
        val raw = min + fraction * (max - min)
        set((raw / step).roundToInt() * step)
    }

    private fun fraction(): Double = if (max <= min) 0.0 else (get() - min).toDouble() / (max - min)
}

/**
 * One-of-many row: click or press Left/Right to cycle. The options are already translated components, so the
 * widget does not need to know whether they come from an enum or a list.
 */
class UiChoice(
    private val labelKey: String,
    private val descriptionKey: String,
    private val options: List<Component>,
    private val selected: () -> Int,
    private val onSelect: (Int) -> Unit,
) : UiWidget() {
    override val focusable: Boolean get() = true

    override fun measure(font: Font): Int = UiMetrics.ROW_HEIGHT

    override fun render(
        ctx: GuiGraphicsExtractor,
        font: Font,
        mouseX: Int,
        mouseY: Int,
    ) {
        rowBackground(ctx, mouseX, mouseY)
        ctx.text(font, Tr.tr(labelKey), x + 4, labelY(font), Theme.argb(Theme.TEXT), false)
        val value = options.getOrNull(selected()) ?: return
        ctx.text(font, value, x + width - 4 - font.width(value), labelY(font), Theme.argb(Theme.TEXT_MUTED), false)
    }

    override fun mouseClicked(event: MouseButtonEvent): Boolean {
        if (!contains(event.x().toInt(), event.y().toInt())) return false
        cycle(1)
        return true
    }

    override fun keyPressed(event: KeyEvent): Boolean {
        if (options.isEmpty()) return false
        when (event.key()) {
            InputConstants.KEY_LEFT -> cycle(-1)
            InputConstants.KEY_RIGHT, InputConstants.KEY_RETURN, InputConstants.KEY_SPACE -> cycle(1)
            InputConstants.KEY_HOME -> onSelect(0)
            InputConstants.KEY_END -> onSelect(options.lastIndex)
            else -> return false
        }
        return true
    }

    override fun tooltip(
        mouseX: Int,
        mouseY: Int,
    ): Component? = if (contains(mouseX, mouseY)) Tr.tr(descriptionKey) else null

    private fun cycle(delta: Int) {
        if (options.isEmpty()) return
        val next = Math.floorMod(selected() + delta, options.size)
        onSelect(next)
    }
}

/**
 * Single-line text field. The label is optional so the same widget serves setting rows and the search box.
 * Editing keeps the caret at the end; [onSubmit] fires on Enter.
 */
class UiTextField(
    private val labelKey: String?,
    private val placeholderKey: String?,
    private val get: () -> String,
    private val onChange: (String) -> Unit,
    private val onSubmit: (() -> Unit)? = null,
    private val descriptionKey: String? = null,
) : UiWidget() {
    override val focusable: Boolean get() = true

    override fun measure(font: Font): Int = if (labelKey == null) 24 else 40

    override fun render(
        ctx: GuiGraphicsExtractor,
        font: Font,
        mouseX: Int,
        mouseY: Int,
    ) {
        if (labelKey != null) ctx.text(font, Tr.tr(labelKey), x + 4, y + 1, Theme.argb(Theme.TEXT), false)
        val fieldY = if (labelKey == null) y + 1 else y + 18
        val fieldHeight = UiMetrics.FIELD_HEIGHT
        val border = if (focused) Theme.BRAND_AQUA else Theme.BORDER
        ctx.fill(x, fieldY, x + width, fieldY + fieldHeight, Theme.argb(Theme.SURFACE_SUNKEN, 230))
        ctx.outline(x, fieldY, width, fieldHeight, Theme.argb(border, if (focused) 200 else 140))

        val value = get()
        val showPlaceholder = value.isEmpty() && placeholderKey != null && !focused
        val text = if (showPlaceholder) Tr.tr(placeholderKey) else Tr.verbatim(value)
        val color = if (value.isEmpty()) Theme.TEXT_SUBTLE else Theme.TEXT
        val textY = fieldY + (fieldHeight - font.lineHeight) / 2
        ctx.text(font, text, x + 6, textY, Theme.argb(color), false)
        if (focused && (System.currentTimeMillis() / 500) % 2 == 0L) {
            val caretX = (x + 6 + font.width(value)).coerceAtMost(x + width - 3)
            ctx.fill(caretX, textY - 1, caretX + 1, textY + font.lineHeight - 1, Theme.argb(Theme.BRAND_AQUA))
        }
    }

    override fun mouseClicked(event: MouseButtonEvent): Boolean = contains(event.x().toInt(), event.y().toInt())

    override fun charTyped(event: CharacterEvent): Boolean {
        if (!event.isAllowedChatCharacter()) return false
        val value = get()
        if (fontWidth(value) >= width - 14) return false
        onChange(value + event.codepointAsString())
        return true
    }

    override fun keyPressed(event: KeyEvent): Boolean {
        if (event.key() == InputConstants.KEY_BACKSPACE) {
            val value = get()
            if (value.isNotEmpty()) onChange(value.substring(0, value.length - 1))
            return true
        }
        if (event.key() == InputConstants.KEY_RETURN) {
            onSubmit?.invoke()
            return onSubmit != null
        }
        return false
    }

    override fun tooltip(
        mouseX: Int,
        mouseY: Int,
    ): Component? = if (descriptionKey != null && contains(mouseX, mouseY)) Tr.tr(descriptionKey) else null

    private fun fontWidth(text: String): Int =
        net.minecraft.client.Minecraft
            .getInstance()
            .font
            .width(text)
}

/** Flat button. `primary` uses the brand colour; the secondary variant is a bordered ghost. */
class UiButton(
    private val labelKey: String,
    private val primary: Boolean,
    private val onClick: () -> Unit,
) : UiWidget() {
    override val focusable: Boolean get() = true

    override fun measure(font: Font): Int = UiMetrics.BUTTON_HEIGHT

    override fun render(
        ctx: GuiGraphicsExtractor,
        font: Font,
        mouseX: Int,
        mouseY: Int,
    ) {
        val hovered = contains(mouseX, mouseY)
        if (primary) {
            ctx.fill(x, y, x + width, y + height, Theme.argb(Theme.BRAND_AQUA, if (hovered) 235 else 205))
            ctx.centeredText(font, Tr.tr(labelKey), x + width / 2, labelY(font), Theme.argb(Theme.SCREEN_DIM))
        } else {
            if (hovered) ctx.fill(x, y, x + width, y + height, Theme.argb(Theme.SURFACE_RAISED, 200))
            ctx.outline(x, y, width, height, Theme.argb(if (hovered) Theme.TEXT_MUTED else Theme.BORDER))
            ctx.centeredText(font, Tr.tr(labelKey), x + width / 2, labelY(font), Theme.argb(if (hovered) Theme.TEXT else Theme.TEXT_MUTED))
        }
        if (focused) ctx.outline(x - 1, y - 1, width + 2, height + 2, Theme.argb(Theme.BRAND_LAVENDER, 160))
    }

    override fun mouseClicked(event: MouseButtonEvent): Boolean {
        if (!contains(event.x().toInt(), event.y().toInt())) return false
        onClick()
        return true
    }

    override fun keyPressed(event: KeyEvent): Boolean {
        if (event.key() != InputConstants.KEY_RETURN && event.key() != InputConstants.KEY_SPACE) return false
        onClick()
        return true
    }
}

/** Horizontal tabs with a brand underline on the active one. */
class UiTabs(
    private val tabKeys: List<String>,
    private val selected: () -> Int,
    private val onSelect: (Int) -> Unit,
) : UiWidget() {
    private var bounds: List<IntRange> = emptyList()

    override val focusable: Boolean get() = true

    override fun measure(font: Font): Int = UiMetrics.TAB_HEIGHT

    override fun layout(
        x: Int,
        y: Int,
        width: Int,
        font: Font,
    ) {
        super.layout(x, y, width, font)
        var cursor = x + 4
        bounds =
            tabKeys.map { key ->
                val start = cursor
                cursor += font.width(Tr.tr(key)) + UiMetrics.TAB_PADDING
                start until cursor
            }
    }

    override fun render(
        ctx: GuiGraphicsExtractor,
        font: Font,
        mouseX: Int,
        mouseY: Int,
    ) {
        for ((index, range) in bounds.withIndex()) {
            val component = Tr.tr(tabKeys[index])
            val hovered = mouseX in range && mouseY >= y && mouseY < y + height
            val color =
                when {
                    index == selected() -> Theme.TEXT
                    hovered -> Theme.BRAND_LAVENDER
                    else -> Theme.TEXT_MUTED
                }
            ctx.text(font, component, range.first, y + 4, Theme.argb(color), false)
            if (index == selected()) {
                ctx.fill(range.first, y + height - 2, range.last - 8, y + height, Theme.argb(Theme.BRAND_AQUA, 200))
            }
        }
        if (focused) ctx.outline(x, y - 2, width, height + 2, Theme.argb(Theme.BRAND_LAVENDER, 90))
    }

    override fun mouseClicked(event: MouseButtonEvent): Boolean {
        if (!contains(event.x().toInt(), event.y().toInt())) return false
        val index = bounds.indexOfFirst { event.x().toInt() in it }
        if (index < 0) return false
        onSelect(index)
        return true
    }

    override fun keyPressed(event: KeyEvent): Boolean {
        if (tabKeys.isEmpty()) return false
        val next =
            when (event.key()) {
                InputConstants.KEY_LEFT -> selected() - 1
                InputConstants.KEY_RIGHT -> selected() + 1
                else -> return false
            }
        onSelect(next.coerceIn(0, tabKeys.lastIndex))
        return true
    }
}
