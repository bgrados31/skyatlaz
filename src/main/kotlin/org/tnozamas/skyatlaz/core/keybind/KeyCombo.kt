package org.tnozamas.skyatlaz.core.keybind

/**
 * A key and its modifiers, the way a player thinks about it: "Shift + F9". Pure data, so it can be compared,
 * stored and tested without the game.
 *
 * [key] is a vanilla key code (`InputConstants.KEY_F9`) and [modifiers] a mask of [SHIFT], [CTRL] and [ALT]. Both
 * travel to the config file as a single number, so a binding is one setting.
 */
data class KeyCombo(
    val key: Int,
    val modifiers: Int = 0,
) {
    /** False when nothing is bound. */
    val isBound: Boolean get() = key != UNBOUND

    /** The number stored in the config: modifiers in the high bits, the key code in the low ones. */
    fun encode(): Int = (modifiers shl MODIFIER_SHIFT) or (key and 0xFFFF)

    /** True when every modifier of this combo is in [pressed]. */
    fun modifiersPressed(pressed: Int): Boolean = pressed and modifiers == modifiers

    companion object {
        /** The key code that means "no key". */
        const val UNBOUND = 0

        const val SHIFT = 1
        const val CTRL = 2
        const val ALT = 4

        private const val MODIFIER_SHIFT = 16
        private const val KEY_MASK = 0xFFFF

        /** Nothing bound. */
        val NONE = KeyCombo(UNBOUND)

        /** Reads a combo back from the number stored in the config. */
        fun decode(value: Int): KeyCombo = KeyCombo(value and KEY_MASK, (value ushr MODIFIER_SHIFT) and KEY_MASK)
    }
}
