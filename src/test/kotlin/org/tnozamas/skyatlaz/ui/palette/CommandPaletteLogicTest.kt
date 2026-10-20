package org.tnozamas.skyatlaz.ui.palette

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** The search of the command palette, without the game. */
class CommandPaletteLogicTest {
    private val entries =
        listOf(
            PaletteEntry("config", "Open SkyAtlaz settings", "/skyatlaz config", false),
            PaletteEntry("hud", "Open the HUD editor", "/skyatlaz hud", false),
            PaletteEntry("debug price <id>", "Show bazaar, auction and NPC prices of an item", "/skyatlaz debug price", true),
            PaletteEntry("version", "Show the installed SkyAtlaz version", "/skyatlaz version", false),
        )

    @Test
    fun `an empty query keeps every command`() {
        assertEquals(entries, CommandPaletteLogic.search(entries, ""))
        assertEquals(entries, CommandPaletteLogic.search(entries, "   "))
    }

    @Test
    fun `a query that is part of a name finds it`() {
        assertEquals(
            listOf(entries[0], entries[2]),
            CommandPaletteLogic.search(entries, "c"),
        )
    }

    @Test
    fun `a full name wins over a fuzzy match in a description`() {
        assertEquals(listOf(entries[1], entries[2]), CommandPaletteLogic.search(entries, "hud"))
    }

    @Test
    fun `the letters only have to appear in order`() {
        val matches = CommandPaletteLogic.search(entries, "cfg")

        assertEquals(listOf(entries[0]), matches)
    }

    @Test
    fun `the description is searched too`() {
        val matches = CommandPaletteLogic.search(entries, "editor")

        assertEquals(listOf(entries[1]), matches)
    }

    @Test
    fun `a name beats a description`() {
        val matches = CommandPaletteLogic.search(entries, "price")

        assertEquals(listOf(entries[2]), matches)
    }

    @Test
    fun `nothing that does not match is offered`() {
        assertTrue(CommandPaletteLogic.search(entries, "zzzz").isEmpty())
    }

    @Test
    fun `the query does not care about the case`() {
        assertEquals(CommandPaletteLogic.search(entries, "hud"), CommandPaletteLogic.search(entries, "HuD"))
    }
}
