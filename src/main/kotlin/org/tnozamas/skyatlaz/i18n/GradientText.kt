package org.tnozamas.skyatlaz.i18n

import net.minecraft.network.chat.Component
import net.minecraft.network.chat.MutableComponent
import org.tnozamas.skyatlaz.ui.theme.Gradient

/**
 * Paints this (already translated) text with [gradient]: `Tr.tr("key").withGradient(Theme.BRAND_GRADIENT)`.
 * The root style (bold, click/hover events) is kept; colors of nested parts are replaced by the gradient.
 *
 * Builds one styled piece per color run, so call it when the text changes and keep the result; never per frame.
 * Lives in `i18n` because it creates components from resolved strings, which only `i18n` may do.
 */
fun Component.withGradient(
    gradient: Gradient,
    offset: Float = 0f,
): MutableComponent {
    val text = string
    val result = Component.empty().withStyle(style)
    gradient.forEachRun(text, offset) { start, end, rgb ->
        result.append(Component.literal(text.substring(start, end)).withColor(rgb))
    }
    return result
}
