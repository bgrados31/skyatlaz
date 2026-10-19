package org.tnozamas.skyatlaz.util

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test

class LegacyTextTest {
    @Test
    fun `strip removes legacy and hex codes`() {
        assertEquals("LEGENDARY Hyperion", LegacyText.strip("§6§lLEGENDARY §r§6Hyperion"))
        assertEquals("Purse: 1,234", LegacyText.strip("§#ffaa00Purse: §61,234"))
    }

    @Test
    fun `strip returns the same instance when there is nothing to strip`() {
        val text = "Purse: 1,234"
        assertSame(text, LegacyText.strip(text))
    }

    @Test
    fun `strip tolerates a dangling section sign`() {
        assertEquals("end§", LegacyText.strip("end§"))
    }

    @Test
    fun `ampersand codes become section codes`() {
        assertEquals("§6Gold §lbold", LegacyText.fromAmpersand("&6Gold &lbold"))
        assertEquals("§cRed", LegacyText.fromAmpersand("&CRed"))
        // A lone `&`, an unknown code and the `&&` escape are all kept as text.
        assertEquals("Tom & Jerry &z & done", LegacyText.fromAmpersand("Tom & Jerry &z && done"))
    }
}
