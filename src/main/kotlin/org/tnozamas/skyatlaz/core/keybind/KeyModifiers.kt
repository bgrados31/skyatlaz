package org.tnozamas.skyatlaz.core.keybind

import com.mojang.blaze3d.platform.InputConstants
import com.mojang.blaze3d.platform.Window

/**
 * The modifier keys the player is holding right now, as a [KeyCombo] mask. Shared by the binding manager (which
 * polls) and the settings row (which captures), so both read the same state.
 */
object KeyModifiers {
    /** The mask of the modifiers that are down. */
    fun pressed(window: Window): Int {
        var modifiers = 0
        if (isDown(window, InputConstants.KEY_LSHIFT) || isDown(window, InputConstants.KEY_RSHIFT)) {
            modifiers = modifiers or KeyCombo.SHIFT
        }
        if (isDown(window, InputConstants.KEY_LCONTROL) || isDown(window, InputConstants.KEY_RCONTROL)) {
            modifiers = modifiers or KeyCombo.CTRL
        }
        if (isDown(window, InputConstants.KEY_LALT) || isDown(window, InputConstants.KEY_RALT)) {
            modifiers = modifiers or KeyCombo.ALT
        }
        return modifiers
    }

    private fun isDown(
        window: Window,
        key: Int,
    ): Boolean = InputConstants.isKeyDown(window, key)
}
