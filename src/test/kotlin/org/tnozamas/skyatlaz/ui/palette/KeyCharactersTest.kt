package org.tnozamas.skyatlaz.ui.palette

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

/** The character a key produces, without the game. */
class KeyCharactersTest {
    @Test
    fun `a letter key produces its letter`() {
        assertEquals('K', KeyCharacters.of(75))
        assertEquals('A', KeyCharacters.of(65))
        assertEquals('Z', KeyCharacters.of(90))
    }

    @Test
    fun `a digit key produces its digit`() {
        assertEquals('0', KeyCharacters.of(48))
        assertEquals('9', KeyCharacters.of(57))
    }

    @Test
    fun `a key without a character has none`() {
        assertNull(KeyCharacters.of(0))
        assertNull(KeyCharacters.of(290))
    }
}
