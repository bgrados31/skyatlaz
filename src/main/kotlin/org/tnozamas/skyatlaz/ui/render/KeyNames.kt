package org.tnozamas.skyatlaz.ui.render

import com.mojang.blaze3d.platform.InputConstants
import net.minecraft.network.chat.Component
import org.tnozamas.skyatlaz.core.keybind.KeyCombo
import org.tnozamas.skyatlaz.i18n.Tr

/**
 * How a key combination is written for the player: "Shift + F9" or "Unbound".
 *
 * The key itself uses the vanilla names ("F9", "E"), so it follows the language of the game; the modifiers and the
 * empty state are ours.
 */
object KeyNames {
    /** The name of a combination. */
    fun of(combo: KeyCombo): Component {
        if (!combo.isBound) return Tr.tr("keybind.unbound")
        val parts = ArrayList<Component>(4)
        if (combo.modifiers and KeyCombo.SHIFT != 0) parts += Tr.tr("keybind.modifier.shift")
        if (combo.modifiers and KeyCombo.CTRL != 0) parts += Tr.tr("keybind.modifier.ctrl")
        if (combo.modifiers and KeyCombo.ALT != 0) parts += Tr.tr("keybind.modifier.alt")
        parts +=
            InputConstants.Type.KEYSYM
                .getOrCreate(combo.key)
                .displayName
        val result = Component.empty()
        for ((index, part) in parts.withIndex()) {
            if (index > 0) result.append(Tr.verbatim(SEPARATOR))
            result.append(part)
        }
        return result
    }

    /** The text between a modifier and the key. */
    private const val SEPARATOR = " + "
}
