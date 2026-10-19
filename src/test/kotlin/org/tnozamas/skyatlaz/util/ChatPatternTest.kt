package org.tnozamas.skyatlaz.util

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ChatPatternTest {
    private val rareDrop =
        ChatPattern("RARE DROP!", """RARE DROP! (?<item>.+?)(?: \(\+(?<mf>[\d,]+)% ✯ Magic Find\))?""")
    private val coins = ChatPattern("coins", """You earned (?<amount>[\d,.]+[kMB]?) coins!""")
    private val level = ChatPattern("Skill Level", """Skill Level (?<level>[IVXLC\d]+)""")

    @Test
    fun `named groups and optional groups`() {
        val withMf = rareDrop.match("RARE DROP! Shadow Assassin Chestplate (+228% ✯ Magic Find)")!!
        assertEquals("Shadow Assassin Chestplate", withMf["item"])
        assertEquals(228.0, withMf.number("mf"))

        val plain = rareDrop.match("RARE DROP! Enchanted Ender Pearl")!!
        assertEquals("Enchanted Ender Pearl", plain["item"])
        assertNull(plain["mf"])
    }

    @Test
    fun `numbers and roman numerals from groups`() {
        assertEquals(1_200_000L, coins.match("You earned 1.2M coins!")!!.long("amount"))
        assertEquals(24, level.match("Skill Level XXIV")!!.roman("level"))
    }

    @Test
    fun `prefilter rejects without running the regex and the regex must match fully`() {
        assertNull(rareDrop.match("Some unrelated message"))
        assertFalse(rareDrop.matches("Guild > Player: RARE DROP! fake"))
        assertTrue(coins.matches("You earned 5 coins!"))
    }

    @Test
    fun `empty prefilter is rejected`() {
        assertThrows(IllegalArgumentException::class.java) { ChatPattern("", ".*") }
    }
}
