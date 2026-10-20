package org.tnozamas.skyatlaz.core.keybind

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** A key and its modifiers, as they travel to the config file. */
class KeyComboTest {
    @Test
    fun `a combo survives the round trip through the config`() {
        val combo = KeyCombo(290, KeyCombo.SHIFT or KeyCombo.CTRL)

        assertEquals(combo, KeyCombo.decode(combo.encode()))
    }

    @Test
    fun `an unbound combo has no key`() {
        assertFalse(KeyCombo.NONE.isBound)
        assertTrue(KeyCombo(290).isBound)
    }

    @Test
    fun `the modifiers of a combo must be held`() {
        val combo = KeyCombo(290, KeyCombo.SHIFT)

        assertTrue(combo.modifiersPressed(KeyCombo.SHIFT))
        assertTrue(combo.modifiersPressed(KeyCombo.SHIFT or KeyCombo.CTRL))
        assertFalse(combo.modifiersPressed(KeyCombo.CTRL))
        assertFalse(combo.modifiersPressed(0))
    }

    @Test
    fun `a combo without modifiers is satisfied by any modifier`() {
        assertTrue(KeyCombo(290).modifiersPressed(KeyCombo.SHIFT))
    }

    @Test
    fun `an empty value decodes to an unbound combo`() {
        assertEquals(KeyCombo.NONE, KeyCombo.decode(0))
    }
}
