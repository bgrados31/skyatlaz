package org.tnozamas.skyatlaz.api.profile

import com.google.gson.JsonParseException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** The mining tree of the profile API, read from a captured answer. */
class ProfileApiTest {
    private val json: String =
        requireNotNull(javaClass.getResource("/fixtures/profile/profile.json")).readText()

    private val uuid = "069a79f4-0000-4000-8000-000000000000"

    private val mining = ProfileApi.parseMining(json, uuid)

    @Test
    fun `the perks of the tree are read`() {
        val fortune = mining.perks.firstOrNull { it.id == "mining_fortune" }

        assertEquals(47, fortune?.level)
        assertTrue(fortune?.enabled == true)
    }

    @Test
    fun `the perks of every sub-tree are put together`() {
        val ids = mining.perks.map { it.id }

        assertTrue(ids.contains("mining_speed"))
        assertTrue(ids.contains("old_school"))
        assertTrue(ids.contains("efficient_miner"))
    }

    @Test
    fun `a perk at its maximum keeps its level`() {
        assertEquals(50, mining.perks.firstOrNull { it.id == "mining_speed" }?.level)
        assertEquals(20, mining.perks.firstOrNull { it.id == "quick_forge" }?.level)
    }

    @Test
    fun `a perk that is switched off is told apart`() {
        val withToggle = ProfileApi.parseMining(json.replace("\"toggle_pickobulus\": true", "\"toggle_pickobulus\": false"), uuid)

        assertFalse(withToggle.perks.firstOrNull { it.id == "pickobulus" }?.enabled == true)
    }

    @Test
    fun `the powders are read`() {
        assertEquals(184521, mining.mithrilPowder)
        assertEquals(96320, mining.gemstonePowder)
        assertEquals(4096, mining.glacitePowder)
    }

    @Test
    fun `an answer that is not a profile is refused`() {
        val error =
            assertThrows(JsonParseException::class.java) {
                ProfileApi.parseMining("""{"success": false, "cause": "Invalid API Key"}""", uuid)
            }

        assertEquals("Invalid API Key", error.message)
    }
}
