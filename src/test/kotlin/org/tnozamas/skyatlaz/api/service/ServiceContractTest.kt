package org.tnozamas.skyatlaz.api.service

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File

/**
 * The contract of the skyatlaz-service. The copy in `docs/api` is refreshed by `tools/service/sync.ps1`, and this
 * test is the alarm that rings when the service and the mod have drifted apart: a field the service adds has to
 * be read by the mod, or the build fails instead of the game.
 */
class ServiceContractTest {
    private val contract: JsonObject =
        JsonParser.parseString(File("docs/api/skyatlaz-service.json").readText()).asJsonObject

    private fun fields(schema: String): Set<String> =
        contract
            .getAsJsonObject("components")
            .getAsJsonObject("schemas")
            .getAsJsonObject(schema)
            .getAsJsonObject("properties")
            .keySet()

    @Test
    fun `the mod reads every field of the profile`() {
        assertEquals(
            setOf("uuid", "name", "mining", "dungeons", "currencies", "fetched_at"),
            fields("SlimProfile"),
        )
    }

    @Test
    fun `the mod reads every field of the mining tree`() {
        assertEquals(setOf("perks", "toggles", "ability", "powder"), fields("MiningProfile"))
    }

    @Test
    fun `the contract has the endpoints the mod uses`() {
        val paths = contract.getAsJsonObject("paths").keySet()

        assertTrue(
            paths.containsAll(listOf("/health", "/v1/auth/challenge", "/v1/auth/verify", "/v1/profile")),
            "the contract does not have the endpoints the mod uses",
        )
    }
}
