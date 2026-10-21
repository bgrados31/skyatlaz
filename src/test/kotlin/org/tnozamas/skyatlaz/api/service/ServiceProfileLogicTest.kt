package org.tnozamas.skyatlaz.api.service

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

/** The profile the skyatlaz service answers with. */
class ServiceProfileLogicTest {
    private val profile: ServiceProfile =
        parseServiceProfile(
            requireNotNull(javaClass.getResource("/fixtures/service/profile.json"))
                .readText()
                .let { JsonParser.parseString(it).asJsonObject },
        )

    @Test
    fun `the profile of the service is read`() {
        assertEquals("01234567-89ab-cdef-0123-456789abcdef", profile.uuid)
        assertEquals("TNOZamas", profile.name)
        assertEquals(1700000000000L, profile.fetchedAt)
    }

    @Test
    fun `the mining tree is read`() {
        assertEquals(50, profile.mining.perks["mining_speed"])
        assertEquals(3, profile.mining.perks["compact_mining"])
        assertEquals(true, profile.mining.toggles["mining_speed"])
        assertEquals(false, profile.mining.toggles["compact_mining"])
        assertEquals("pickobulus", profile.mining.ability)
        assertEquals(1234, profile.mining.powder["mithril"])
        assertEquals(56, profile.mining.powder["gemstone"])
    }

    @Test
    fun `the other sections are passed through`() {
        assertEquals(
            3,
            profile.dungeons
                ?.get("catacombs")
                ?.asJsonObject
                ?.get("completed")
                ?.asInt,
        )
        assertEquals(42, profile.currencies?.get("coin")?.asInt)
    }

    @Test
    fun `an answer without a mining tree does not break the reader`() {
        val empty = parseServiceProfile(JsonObject())

        assertEquals(emptyMap<String, Int>(), empty.mining.perks)
        assertEquals(emptyMap<String, Boolean>(), empty.mining.toggles)
        assertEquals(emptyMap<String, Int>(), empty.mining.powder)
        assertNull(empty.mining.ability)
        assertNull(empty.name)
    }
}
