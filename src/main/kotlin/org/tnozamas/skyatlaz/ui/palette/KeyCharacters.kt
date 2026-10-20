package org.tnozamas.skyatlaz.ui.palette

/**
 * The character a key produces, for the keys that produce one: the letters (GLFW 65..90) and the digits (48..57).
 * Keys without a character (function keys, arrows...) return null.
 *
 * Pure, so it can be tested without the game.
 */
object KeyCharacters {
    fun of(keyCode: Int): Char? =
        when (keyCode) {
            in 65..90 -> 'A' + (keyCode - 65)
            in 48..57 -> '0' + (keyCode - 48)
            else -> null
        }
}
