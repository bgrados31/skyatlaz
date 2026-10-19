package org.tnozamas.skyatlaz.util

import net.minecraft.network.chat.Component
import net.minecraft.network.chat.Style
import java.util.Optional

/**
 * Hypixel still thinks in legacy `§` formatting codes: chat patterns, scoreboard lines and item rarities are easiest
 * to match against `§6§lLEGENDARY`. This converts game text to that form and back to plain text. Parsers match
 * on [strip] output unless they need colors.
 *
 * RGB colors (not expressible as legacy codes) are written as `§#rrggbb`, which [strip] also removes.
 */
object LegacyText {
    const val SECTION = '§'
    private const val RESET = "§r"
    private const val CODES = "0123456789abcdefklmnor"
    private const val HEX_LENGTH = 7

    /** Vanilla color names in legacy code order (`black` = `§0` ... `white` = `§f`). */
    private val COLOR_NAMES =
        listOf(
            "black",
            "dark_blue",
            "dark_green",
            "dark_aqua",
            "dark_red",
            "dark_purple",
            "gold",
            "gray",
            "dark_gray",
            "blue",
            "green",
            "aqua",
            "red",
            "light_purple",
            "yellow",
            "white",
        )

    /** Removes every formatting code (`§x` and `§#rrggbb`). Allocation-free when there is nothing to strip. */
    fun strip(text: String): String {
        if (text.indexOf(SECTION) < 0) return text
        val result = StringBuilder(text.length)
        var index = 0
        while (index < text.length) {
            val char = text[index]
            if (char == SECTION && index + 1 < text.length) {
                index += if (text[index + 1] == '#' && index + HEX_LENGTH < text.length) HEX_LENGTH + 1 else 2
                continue
            }
            result.append(char)
            index++
        }
        return result.toString()
    }

    /** `&6Gold &lbold` -> `§6Gold §lbold`, for typing colored test messages. `&&` stays a literal `&`. */
    fun fromAmpersand(text: String): String {
        val result = StringBuilder(text.length)
        var index = 0
        while (index < text.length) {
            val char = text[index]
            val next = text.getOrNull(index + 1)
            if (char == '&' && next != null && next.lowercaseChar() in CODES) {
                result.append(SECTION).append(next.lowercaseChar())
                index += 2
            } else if (char == '&' && next == '&') {
                result.append('&')
                index += 2
            } else {
                result.append(char)
                index++
            }
        }
        return result.toString()
    }

    /** Game text as a legacy string, emitting codes only where the style changes. */
    fun encode(component: Component): String {
        val result = StringBuilder()
        var previous: Style? = null
        component.visit<Unit>(
            { style, text ->
                if (text.isNotEmpty()) {
                    if (style != previous) {
                        appendCodes(result, style, reset = previous != null)
                        previous = style
                    }
                    result.append(text)
                }
                Optional.empty()
            },
            Style.EMPTY,
        )
        return result.toString()
    }

    private fun appendCodes(
        out: StringBuilder,
        style: Style,
        reset: Boolean,
    ) {
        val color = style.color?.serialize()
        if (color != null) {
            val index = COLOR_NAMES.indexOf(color)
            if (index >= 0) out.append(SECTION).append(CODES[index]) else out.append(SECTION).append(color.lowercase())
        } else if (reset) {
            out.append(RESET)
        }
        if (style.isObfuscated) out.append(SECTION).append('k')
        if (style.isBold) out.append(SECTION).append('l')
        if (style.isStrikethrough) out.append(SECTION).append('m')
        if (style.isUnderlined) out.append(SECTION).append('n')
        if (style.isItalic) out.append(SECTION).append('o')
    }
}
